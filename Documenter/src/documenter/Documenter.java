/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documenter;

import documenter.Classes.FilenameVersionFf;
import documenter.Classes.ItemData;
import documenter.Classes.ItemUsage;
import documenter.Models.ConfigModel;
import documenter.Services.ConfigurationSvc;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John
 */
public class Documenter {

    private Integer outputtedChunks = 0;
    private Integer processedChunks = 0;
    
    private static final String NewLine = "\n";

    private static final String AppName = "FinancialForce.com Documentation Generator";
    private static final String AppVersion = "v12.0 (16/08/2014)";
    private static final String AppCopyright = "FinancialForce.com © 2014";

    private boolean globalAbort = false;
    private String globalAbortText = "";

    private ConfigModel _configModel;

    private final java.util.ArrayList<ItemData> alItems = new java.util.ArrayList<>();
    private final java.util.ArrayList<ItemUsage> alItemUsage = new java.util.ArrayList<>();
    private final Map<String, String> snippetText = new HashMap<>();

    private static final String ctVar = "Variables";
    private static final String ctUnknown = "Unknown";
    private static final String ctTest = "Tests";
    private static final String ctMethod = "Methods";
    private static final String ctWebServices = "Web Services";
    private static final String ctInterfaces = "Interfaces";
    private static final String ctClass = "Classes/Types";
    private static final String ctEnum = "Enumerations";
    private static final String ctComment = "Comment";
    private static final String ctProperty = "Property";
    private static final String ctRemoteAction = "RemoteAction";
    
    private final String IndexFolder = "Content";
    private final String TocFolder = "Projects\\Tocs";
    
    private final String EnumsFolder = "Content\\Reference\\Enums";
    private final String ServicesFolder = "Content\\Reference\\Services";
    private final String TypesFolder = "Content\\Reference\\Types";
    private final String TestsFolder = "Content\\Reference\\Tests";
    private final String UnknownFolder = "Content\\Reference\\Unknown";
    private final String InterfacesFolder = "Content\\Reference\\Interfaces";
    
    private final String SnippetsFolder = "Content\\Resources\\Snippets";
    
       
    private static final String DefFileExt = ".htm";

    
    //**************************************************************************
    // Routine: Main
    // @param args the command line arguments
    //  
    //**************************************************************************
    public static void main(String[] args) {
        Documenter ceEngine = new Documenter();

//        if (args.length >= 1) //Do we have a first parameter (config file)?
//        {
//            sConfigFile = args[0];		//Yes - Store its name
//        }
        String configFilename = "D:\\Dev_GitHub\\Documenter\\Documenter\\src\\ConfigFilesEtc\\V12.dcf";

        //Output copyright info, version etc.
        System.out.printf("%s %s\n%s\n\n", AppName, AppVersion, AppCopyright);

        //Tell the user that we've started...
        System.out.printf("Generation Started: %s\n\n", new Date().toString());

        //...and *start* (by loading the configuration)
        ceEngine.loadConfiguration(configFilename);

        if (ceEngine.globalAbort) //Do we have a global abort?
        {
            System.out.printf("\n***ABORT***\n\nDebug: %s\n", ceEngine.globalAbortText);
        } else {																				//No - Run the rest of the process
//            if (args.length >= 2) //Do we have a second parameter (override output path)? 
//            {
//                ceEngine.outputFolder = args[1];		//Yes - Use it 
//            }
            //ceEngine.outputFolder = "C:\\Documenter\\Output\\FFA_12_Java";

            ceEngine.generate();
        }

        //We're finished...so tell the user
        System.out.printf("Generation Completed: %s\n", new Date().toString());
    }
    
    //**************************************************************************
     // Given an input path, this routine looks for .CLS files, scans them
     // (adding the results to the alItems arraylist) and then generates the
     // output documentation files
     //**************************************************************************
    public void generate() {
        
        // Create the output structure
        createOutputStructure(_configModel.OutputFolder);
        
        List<FilenameVersionFf> latestVersionFileList = scanFiles();
        if(latestVersionFileList.isEmpty()){
            globalAbort = true;
            globalAbortText = "Could not find any files to process";
            debugOut( String.format("ABORTED: %s", globalAbortText));
            return;
        }
        processSelectedFiles(latestVersionFileList);
        
        generateOutputDocs(TypesFolder);
        generateFiles();
        
        String message = String.format("Chunks: Processed %s Outputted %s", processedChunks, outputtedChunks);
        debugOut(message);
        System.out.printf(message + NewLine);
    }

    //**************************************************************************
    // Method: loadConfiguration
    // 
    //
    //**************************************************************************
    private void loadConfiguration(String configurationFileName) {
        _configModel = ConfigurationSvc.LoadConfigFile(configurationFileName);
    }

    //**************************************************************************
    // Method: scanFile
    // Scan for the file that we are interested in and select the latest versions 
    //
    //**************************************************************************
    private List<FilenameVersionFf> scanFiles() {
        Integer totalFiles = 0;

        List<FilenameVersionFf> latestVersionFileList = new ArrayList<>();
        
        alItems.clear();

        populateSnippetText();

        if (!_configModel.SourceFolder.endsWith("\\")) //Does our input folder end with a '\'?
        {
            _configModel.SourceFolder += "\\";
        }
        //Get a list of files in the input folder
        File f = new File(_configModel.SourceFolder);

        FilenameFilter textFilter = new FilenameFilterImpl();

        File[] listOfFiles = f.listFiles(textFilter);

        //Do we have any files?
        if (listOfFiles == null)
        {
            System.out.println("Invalid source path");
        } else {
            for (File file : listOfFiles) {
                //Is this item a file?
                if (file.isFile()) {
                    String filename = file.getName();
                    String[] filenameSplitStrings = filename.split("_");
                    int version = 1;
                    // This happens when there is version information in the file name
                    if (filenameSplitStrings.length > 2) {
                        version = Integer.parseInt(filenameSplitStrings[1]);
                    } else {
                        // Get the class name
                        int pos = filename.indexOf('.');
                        filenameSplitStrings[0] = filename.substring(0, pos);
                    }
                    
                    FilenameVersionFf filenameVersionFf = new FilenameVersionFf();
                    filenameVersionFf.Name = filenameSplitStrings[0];
                    filenameVersionFf.FullName = file.getPath();
                    filenameVersionFf.Version = version;
                    
                    int result = getIndexOf(latestVersionFileList, filenameVersionFf);
                    if (-1 == result) {
                        // Add
                        latestVersionFileList.add(filenameVersionFf);
                    } else {
                        // Check to seee if we are newer if so update
                        if (filenameVersionFf.Version > latestVersionFileList.get(result).Version) {
                            latestVersionFileList.set(result, filenameVersionFf);
                        } else {
                            //Debug.WriteLine(filenameVersionFf.FullName);
                        }
                    }
                }
                dumpFiles(latestVersionFileList);
                if (globalAbort) //Did we abort?
                {
                    break;
                }
            }
        }
        return latestVersionFileList;
    }

    //**************************************************************************
    // Routine: processSelectedFiles
    //
    //**************************************************************************
    private void processSelectedFiles(List<FilenameVersionFf> latestVersionFileList){
        Integer totalFiles = 0;
        int notScanned = 0;
        
        List<String> excludeList = _configModel.AllExcludeFiles;
            for (FilenameVersionFf fileName : latestVersionFileList) {
                String name = fileName.Name;
                if (name != null) {
                    name = name.toLowerCase().trim();

//                    String testCatch = "CODAAPIGeneralLedgerAccountTypes";
//                    if (name.toUpperCase().startsWith(testCatch.toUpperCase())) {
//                        DebugOut(name);
//                    }

                    if (!excludeList.contains(name)) {
                        totalFiles++;
                        scanFile(fileName.FullName);
                    } else {
                        notScanned++;
                        System.out.printf(fileName.FullName + " not scaned " + notScanned);
                    }
                }
            }
    }
    
    //**************************************************************************
    // Routine: scanFile
    // Scan for the file that we are interested in and select the latest versions 
    //
    //**************************************************************************
    private void scanFile(String filename) {
        String fileData = "";
        boolean bAbort = false;

        System.out.printf("Scanning: %s%s", filename, NewLine);

        //Load the *whole file* in (in one go)
        BufferedReader inFile;
        try {
            inFile = new BufferedReader(new FileReader(filename));

            String line;
            while ((line = inFile.readLine()) != null) {
                fileData = fileData + line + NewLine;
            }

            inFile.close();
        } catch (FileNotFoundException e) {
            globalAbortText = "FileNotFound (Scan): " + filename + " Error: " + e.getMessage();
            globalAbort = true;
        } catch (IOException e) {
            globalAbortText = "IOException (Scan): " + filename + " Error: " + e.getMessage();
            globalAbort = true;
        }

        if (globalAbort) //Have we aborted?
        {
            return;				//Yes - Get out
        }
        //Remove the scan path
        filename = filename.substring(_configModel.SourceFolder.length());
        //Check whether the filename starts with '\'...
        if (filename.startsWith("\\")) {
            //...and remove it if it does
            filename = filename.substring(1);
        }

        //Add spaces to any CRs and/or tabs we find (it makes pattern matching easier)
        fileData = fileData.replace("\t", " \t ");
        fileData = fileData.replace("\n", " \n ");

        String debugText = "";
        int repeatCount = 0;
        
        //Loop until we run out of data (or get told to abort)...
        while (!fileData.trim().equals("") && !bAbort && !globalAbort) {
            //To stop ourselves form getting stuck in loops, we'll keep a track
            //of the data to see if it changes across iterations. If it *doesn't*
            //then we can spot it and do something about it
            String oldData = fileData;
            //Build a debug 'chain' (which should help us to figure out where we got stuck) 
            debugText += " 0->";

            //Extract the next 'chunk' of data
            RefObject<String> tempRef_sData = new RefObject<>(fileData);
            String sChunkType = "";
            RefObject<String> tempRef_sChunkType = new RefObject<>(sChunkType);

            //Get the next 'chunk'
            String chunk = getNextChunk(tempRef_sData, tempRef_sChunkType);
            fileData = tempRef_sData.argvalue;
            String chunkType = tempRef_sChunkType.argvalue;

            debugText += "1->";

            //Has the data changed (i.e. did we process anything)?
            if (fileData.equals(oldData)) {
                //No - We *might* have a problem
                //Keep a track of how many times the data *hasn't* changed
                repeatCount += 1;
                debugText = debugText + "[Loop " + repeatCount + "]";

                //Have we looped 10 times?
                if (repeatCount >= 10) {
                    //Yes - We have a definite problem so...abort
                    bAbort = true;
                    debugText = debugText + " Data: " + fileData;
                }
            } else {
                //Yes - Reset our 'stuck' counter
                repeatCount = 0;
                debugText = "";
            }

            //Is the chunk empty?
            if (!chunk.equals("")) {
                //No - Process it

                String check;
                //If the chunk contains a CR...
                if (chunk.contains("\n")) {
                    //...get everything *up to* the CR...
                    check = chunk.substring(0, chunk.indexOf("\n"));
                } else {
                    //...otherwise use the entire chunk
                    check = chunk;
                }

                //Is this a class?
                //if (sCheck.toLowerCase().contains("with sharing class"))
                if (check.toLowerCase().contains(" class ")) {
                    processChunk(chunk, "", filename);
                } else {
                    //No - Just output/save it
                    outputChunk(chunkType, chunk, "", filename);

                    if (false == chunkType.equals(ctComment)) {
                        String message = String.format("%s %s we are not checking for not marked as with sharing", chunkType, filename);
                        debugOut(message);
                    }
                }
            }
        }

        if (bAbort) //Did we abort?
        {
            //Yes - Set the global flag (so that we don't process anything else anywhere else)
            globalAbort = true;
            globalAbortText = debugText;
        }
    }
    
    //**************************************************************************
    // Routine: CreateOutputStructure
    // Outline: When called, this routine attempts to create the output folder
    //			structure used to hold the generated  
    //**************************************************************************
    private void createOutputStructure(String sFolder) {
        System.out.print("Creating output folders...");

        sFolder = sFolder.trim();

        (new File(sFolder)).mkdir(); //Create the root output folder

        if (!sFolder.endsWith("\\")) //Does our output folder end with a '\'?
        {
            sFolder = sFolder + "\\"; //Nope - Add one on
        }
        //Create the individual item type output folders
        (new File(sFolder + EnumsFolder)).mkdirs();
        (new File(sFolder + InterfacesFolder)).mkdirs();
        (new File(sFolder + ServicesFolder)).mkdirs();
        (new File(sFolder + TestsFolder)).mkdirs();
        (new File(sFolder + TypesFolder)).mkdirs();
        (new File(sFolder + UnknownFolder)).mkdirs();
        (new File(sFolder + TocFolder)).mkdirs();
        (new File(sFolder + SnippetsFolder)).mkdirs();
        System.out.println("Done");
    }
    
    private void generateOutputDocs(String OutputFolder) {

    }

    private void generateFiles() {

    }

    private void dumpFiles(List<FilenameVersionFf> latestVersionFileList) {
        String filesProcessedFileName = _configModel.OutputFolder + "\\FilesProcessed.txt";

        //Does the output file already exist?
        if ((new File(filesProcessedFileName)).isFile()) {
            (new File(filesProcessedFileName)).delete();
        }

        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filesProcessedFileName))) {
                for (FilenameVersionFf file : latestVersionFileList) {
                    writer.write(file.FullName + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateSnippetText() {
        // Add some elements to the dictionary. 
        snippetText.put("prop-Id", "JOHN_101");
        snippetText.put("prop-IsDeleted", "JOHN_YES");
        snippetText.put("prop-CreatedById", "JOHN_CreatedBy");
        snippetText.put("prop-CreatedDate", "JOHN_Created_Date");
        snippetText.put("prop-LastModifiedById", "JOHN_LastModifiedById");
        snippetText.put("prop-LastModifiedDate", "JOHN_LastModifiedDate");
        //snippetText.Add("prop-SystemModStamp", "JOHN_SystemModStamp");
        snippetText.put("prop-OwnerId", "JOHN_OwnerId");
    }

    private void debugOut(String message) {
        String debugFileName = _configModel.OutputFolder + "\\Debug.txt";

        try {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ss a");

            File file = new File(debugFileName);
            // creates the file

            if (file.exists() == false) {
                file.createNewFile();
            }

            // Writes the content to the file
            try ( // creates a FileWriter Object
                    FileWriter writer = new FileWriter(file, true)) {
                // Writes the content to the file
                writer.write(ft.format(date) + "  " + message + NewLine);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getIndexOf(List<FilenameVersionFf> latestVersionFileList, FilenameVersionFf filenameVersionFf) {
        if (latestVersionFileList == null) {
            return -1;
        }

        int index = -1;
        for (FilenameVersionFf file : latestVersionFileList) {
            if (file.Name.equals(filenameVersionFf.Name)) {
                index = latestVersionFileList.indexOf(file);
            }
        }
        return index;
    }

    //**************************************************************************
    // Method: getNextChunk
    // Given a set of data, this routine works out the next 'chunk' 
    // (i.e. 'section', structure etc.), decides what 'type' it is
    // (e.g. structure, method) and returns it 
    //**************************************************************************
    private String getNextChunk(RefObject<String> data, RefObject<String> chunkType) {
        String result = "";
        boolean bHandled = false;

        //Assume that we have no idea what this chunk is (as we don't at this point)
        chunkType.argvalue = ctUnknown;

        //Does the data start with a '//' (i.e. is this a comment)?
        if (data.argvalue.startsWith("//"))
        {
            //Yes - Extract the comment, set the return type...and get out
            if (data.argvalue.indexOf("\n") >= 0) {
                result = extractText(data, data.argvalue.indexOf("\n"));
            } else {
                result = data.argvalue;
                data.argvalue = "";
            }

            chunkType.argvalue = ctComment;
            return result;
        }

        //Does the data start with '/*' (i.e. is this a comment)?
        if (data.argvalue.startsWith("/*"))
        {
            //Yes - Extract the comment, set the return type...and get out
            result = extractText(data, data.argvalue.indexOf("*/") + 1);
            chunkType.argvalue = ctComment;
            return result;
        }

        String start;
        //Does this data have a space in it?
        if (data.argvalue.indexOf(" ") >= 0)
        {
             	//Yes - Extract the first 'word'
            start = extractText(data, data.argvalue.indexOf(" "));
        } else {
            //No - Use everything as the first 'word'
            start = data.argvalue;
            data.argvalue = "";
        }

        //Work out what the first 'word' (sStart) contains...and then respond accordingly
        String check = start.toUpperCase().trim();
        int iChunkEnd = -1;
        
        //Check for 'Global', 'Private', 'Public' and '@IsTest' - these should be the only valid ways to start something in Apex
        if ((check.equals("GLOBAL") || check.equals("WEBSERVICE") || check.equals("PUBLIC") || check.equals("PRIVATE") || check.equals("@ISTEST") || check.equals("@REMOTEACTION")) && !bHandled) {
            result = start + " ";

            //Try to figure out the structure of the data by looking for brackets, semi-colons etc.
            int iBracket = data.argvalue.indexOf("(");
            int iBrace = data.argvalue.indexOf("{");
            int iEquals = data.argvalue.indexOf("=");
            int iSemiColon = data.argvalue.indexOf(";");

            //If we can't find brackets...set their position to be stupidly high (it helps the checks later on)
            if (iBracket < 0) {
                iBracket = Integer.MAX_VALUE;
            }
            if (iBrace < 0) {
                iBrace = Integer.MAX_VALUE;
            }

            //Do we have a semi-colon?
            if (iSemiColon != -1)
            {
                //Yes - Check to see if it falls *before* the other things that we scanned for
                if (iEquals > iSemiColon) {
                    iEquals = -1;
                }
                if (iBracket > iSemiColon) {
                    iBracket = -1;
                }
                if (iBrace > iSemiColon) {
                    iBrace = -1;
                }
            }

            if (iEquals != -1) //Variable?
            {
                //Yes - Read to the next semi-colon
                //Is the equals *after* a brace bracket?
                if ((iBrace < iEquals) && (iBrace != -1))
                {
                    iChunkEnd = 2;
                    //Yes - Class or routine

                     //Regular bracket before brace bracket?
                    if ((iBracket < iBrace) && (iBracket != -1))
                    {
                        chunkType.argvalue = ctMethod;
                    } else {
                        chunkType.argvalue = ctClass;
                    }
                } else {
                    //No - Simple variable
                    iChunkEnd = 0;
                    chunkType.argvalue = ctVar;
                }
            } else {
                 //Brace bracket before regular bracket?
                if ((iBrace < iBracket) && (iBrace != -1))
                {
                    //Yes - Enumeration
                    iChunkEnd = 1;
                    chunkType.argvalue = ctEnum;
                } else {
                    //Regular bracket before brace bracket?
                    if ((iBracket < iBrace) && (iBracket != -1)) 
                    {
                        iChunkEnd = 2;
                        chunkType.argvalue = ctMethod;
                    } else {
                        //Brace and *no* regular bracket?
                        if ((iBrace != -1) && (iBracket == -1))
                        {
                            iChunkEnd = 2;
                            chunkType.argvalue = ctProperty;
                        } else {
                             //No brace, no bracket and no equals?
                            if ((iBrace == -1) && (iBracket == -1))
                            {
                                iChunkEnd = 0;
                                chunkType.argvalue = ctVar;
                            }
                        }
                    }
                }
            }

            //Have we found a nested class?
            if ((iBrace > 0)
                    && (data.argvalue.toLowerCase().indexOf("class") < iBrace)
                    && (data.argvalue.toLowerCase().indexOf("class") >= 0))
            {
                //Yes - Read to the end of it
                iChunkEnd = 2;
                chunkType.argvalue = ctClass + "3";
            }

            if (check.equals("@REMOTEACTION"))
                {
                    chunkType.argvalue = ctRemoteAction;
                    result = "";
                }
            
            //If this method is actually a test...
            if ((chunkType.argvalue.equals(ctMethod)) && (check.equals("@ISTEST")))
            {
                //...mark it as such
                chunkType.argvalue = ctTest;
            }
            
            if ((chunkType.argvalue.equals(ctMethod)) && (check.equals("WEBSERVICE")))
                {
                    chunkType.argvalue = ctWebServices;
                }
                
            if (data.argvalue.toUpperCase().trim().startsWith("INTERFACE"))
            {
                chunkType.argvalue = ctInterfaces;
            }
            
            switch (iChunkEnd) {
                case 0: //Scan for next semi-colon
                    result += extractText(data, iSemiColon);
                    bHandled = true;
                    break;
                case 1: //Scan for next close brace bracket
                    result += extractText(data, data.argvalue.indexOf("}"));
                    bHandled = true;
                    break;
                case 2: //Scan for nested close brace bracket
                    result += extractText(data, iBrace);

                    //Loop until we find the end of the routine
                    int iIndent;
                    do {
                        check = extractText(data, data.argvalue.indexOf("}"));

                        result = result + " " + check;

                        iIndent = textCount(result, "{");
                        iIndent -= textCount(result, "}");
                    } while (iIndent > 0);
                    break;
            }

            //Check to see if this thing is an enum...and set it as such (if it is)
            if ((chunkType.argvalue.equals(ctClass))
                    && (!result.contains(";") && (result.toLowerCase().contains(" enum ")))) {
                chunkType.argvalue = ctEnum;
            }
        }
        return result;
    }

    //******************* FROM DOCUMENTERWIZ ***********************************
    // Method: processChunk
    // Given a set of data, this routine works out the next 'chunk' 
    // (i.e. 'section', structure etc.), decides what 'type' it is
    // (e.g. structure, method) and returns it 
    //**************************************************************************
    private void processChunk(String data, String parentClass, String filename) {
        processedChunks++;
        String sChunkType = "";
        String nextChunk = "";
        String chunkType = "";
        String sCheck = "";
        String sClass = "";

        while (data.trim() != "") {
            //Extract the next 'chunk' of data
            RefObject<String> tempRef_sData = new RefObject<String>(data);
            RefObject<String> tempRef_sChunkType = new RefObject<String>(sChunkType);

            //Get the next 'chunk'
            nextChunk = getNextChunk(tempRef_sData, tempRef_sChunkType);
            data = tempRef_sData.argvalue;
            sChunkType = tempRef_sChunkType.argvalue;

            //Is the chunk empty?
            if (!nextChunk.equals("")) {
                //If the chunk contains a new line
                if (nextChunk.contains("\n")) {
                    //...get everything *up to* the CR...
                    sCheck = nextChunk.substring(0, nextChunk.indexOf("\n"));
                } else {
                    //...otherwise use the entire chunk
                    sCheck = nextChunk;
                }
                //Work out if this is a class (or not)
                if ((((sCheck.toLowerCase().contains("with sharing class ") || sCheck.toLowerCase().contains("global class ")) || (sCheck.toLowerCase().contains("public class ")
                        || sCheck.toLowerCase().contains("private class "))) || (((sCheck.toLowerCase().contains("global virtual class ") || sCheck.toLowerCase().contains("public virtual class "))
                        || (sCheck.toLowerCase().contains("private virtual class ") || sCheck.toLowerCase().contains("global abstract class "))) || (sCheck.toLowerCase().contains("public abstract class ")
                        || sCheck.toLowerCase().contains("private abstract class ")))) && (chunkType != "Comment")) {
                    RefObject<String> tempRef_sChunk = new RefObject<String>(nextChunk);
                    sClass = extractText(tempRef_sChunk, nextChunk.indexOf("{") + 1).trim();
                    nextChunk = tempRef_sChunk.argvalue;

                    if (nextChunk.endsWith("}")) {
                        nextChunk = nextChunk.substring(0, nextChunk.length() - 1);
                    }
                    //Output/save the class itself
                    outputChunk(ctClass, sClass, parentClass, filename);

                    //Strip out the class name so that we can prepend it onto any subclasses
                    String str4 = sClass.substring(sClass.toUpperCase().indexOf("CLASS ") + 6).trim();
                    if (str4.contains(" ")) {
                        str4 = str4.substring(0, str4.indexOf(" ")).trim();
                    }
                    str4 = str4.trim();
                    if (str4.endsWith("{")) {
                        str4 = str4.substring(0, str4.length() - 1).trim();
                    }
                    if (parentClass.trim() == "") {
                        processChunk(nextChunk, str4, filename);
                    } else {
                        processChunk(nextChunk, parentClass + "." + str4, filename);
                    }
                } else {
                    outputChunk(chunkType, nextChunk, parentClass, filename);
                }
            }
        }
    }

    //**************************************************************************
    // Method: outputChunk
    // 
    // 
    // 
    //**************************************************************************
    private void outputChunk(String sChunkType, String sChunk, String sParentClass, String sFilename) {
        outputtedChunks++;
        
        if (sChunk.trim() != "")
            {
                String str;
                
                if (((sChunkType.equals("Comment")) || (sChunkType.equals("Unknown"))) || (sChunkType.equals("Tests")))
                {
                    str = "N/A";
                }
                else
                {
                    if (sChunk.toUpperCase().startsWith("STATIC "))
                    {
                        sChunk = sChunk.substring(7).trim();
                    }
                                        
                    if (sChunk.trim().contains(" ")) {
                        str = sChunk.substring(0, sChunk.indexOf(" ")).toUpperCase();
                    } else {
                        str = sChunk.toUpperCase().trim();
                    }
                }

                if ((((str.equals("PUBLIC")) && _configModel.ShowPublic) || ((str.equals("PRIVATE")) && _configModel.ShowPrivate)) 
                    || ((((str.equals("GLOBAL")) && _configModel.ShowGlobal) || ((str.equals("WEBSERVICE")) && _configModel.ShowWebService)) || (str.equals("N/A"))))
                {
                    ItemData newItem = new ItemData();
                    newItem.sName = "";
                    newItem.sParentClass = sParentClass;
                    newItem.sChunkType = sChunkType;
                    newItem.sChunkData = sChunk;
                    newItem.sVisibility = str;
                    newItem.sSourceFile = sFilename;
                    newItem.iInstance = 1;


                    switch (sChunkType)
                    {
                        case ctClass:
                            newItem = decodeClass(newItem);
                            break;

                        case ctEnum:
                            newItem = decodeEnum(newItem);
                            break;

                        case ctVar:
                            newItem = decodeVar(newItem);
                            break;

                        case ctMethod:
                            newItem = decodeMethod(newItem);
                            break;

                        case ctWebServices:
                            newItem = decodeMethod(newItem);
                            break;

                        case ctUnknown:
                            newItem = decodeUnknown(newItem);
                            break;

                        case ctComment:
                            newItem = decodeComment(newItem);
                            break;

                        case ctProperty:
                            newItem = decodeProperty(newItem);
                            break;

                        case ctTest:
                            newItem = decodeTest(newItem);
                            break;

                        case ctInterfaces:
                            newItem = decodeInterface(newItem);
                            break;

                        case ctRemoteAction:
                            newItem.sChunkType = "Methods";
                            newItem.RemoteAction = true;
                            newItem = decodeMethod(newItem);
                            break;
                    }
                    
                    //Did we identify a name for this chunk?
                    if (newItem.sName.trim() != "")
                    {
                        for (ItemData data2 : alItems)
                        {
                            if (((data2.sChunkType == newItem.sChunkType) && (data2.sName.toUpperCase().trim() == newItem.sName.toUpperCase().trim())) && (data2.iInstance >= newItem.iInstance))
                            {
                                newItem.iInstance = data2.iInstance + 1;
                            }
                        }
                        newItem.sOutputFile = newItem.sName + newItem.iInstance + ".htm";
                        alItems.add(newItem);
                    }
                }
            }
    }
    
    //**************************************************************************
    // Method: DecodeClass
    // Given a set of object data containing a class, this 
    // routine extracts the class name and whether it extends another class
    // 
    //**************************************************************************
    private ItemData decodeClass(ItemData newItem) {
        newItem.sName = extractItemName(newItem, "CLASS");

        //Work out if this class extends another class...and store it if it does
        if (newItem.sChunkData.toLowerCase().contains(" extends ")) //Does the chunk data contain 'extends'?
        { 																//Yes - Work out *what* it extends
            newItem.sExtends = newItem.sChunkData.substring(newItem.sChunkData.toLowerCase().indexOf(" extends ") + 8).trim();

            if (newItem.sExtends.endsWith("{")) {
                newItem.sExtends = newItem.sExtends.substring(0, newItem.sExtends.length() - 1).trim();
            }
        }

        return newItem;
    }
    
    //**************************************************************************
    // Routine: DecodeEnum
    // Outline: Given a set of object data containing an 
    //			enumeration, this routine extracts the enumeration name and its
    //			values
    //**************************************************************************
    private ItemData decodeEnum(ItemData newItem) {
        newItem.sName = extractItemName(newItem, "ENUM");

        String str = preProcessComments(newItem.sChunkData, true);

        //Extract the data between the two brace-brackets - this will contain the available values for the enum
        str = str.substring(str.indexOf("{") + 1).trim();
        str = str.substring(0, str.indexOf("}")).trim().replace("\n", " ").replace("\t", " ");
        String[] strArray = str.split("\\,");

        ArrayList<String> alData = new ArrayList<String>();

        //Put the list of values into an ArrayList...
        for (int iCount = 0; iCount < strArray.length; iCount++) {
            alData.add(strArray[iCount].trim());
        }

        java.util.Collections.sort(alData);

        // Iterate through the ArrayList and use it to construct a string of all possible values
        for (int index = 0; index < alData.size(); index++) {
            if (index == 0) {
                str = alData.get(index).toString();
            } else {
                str = str + ", " + alData.get(index).toString();
            }
        }
        newItem.sValues = str;
        return newItem;
    }
    
    //**************************************************************************
    // Method: DecodeVar
    // Given a set of object data containing a variable
    // definition, this routine extract the variable's name and type
    //**************************************************************************
    private ItemData decodeVar(ItemData newItem) {
        String sData = newItem.sChunkData;

        if (sData.contains("=")) //Does the data a have a '=' in it?
        {
            //Yes - The variable has a default value...so get everything up to the '='
            sData = sData.substring(0, sData.indexOf("=") + 1).trim();
            sData = sData.replace("=", "").trim();
        } else
        {
            //No - The variable is *just* the variable...so get everything up to the semi-colon
            sData = sData.replace(";", "").trim();
        }

         //Does the remaining data have a space in it?
        if (sData.contains(" "))
        {
            //Yes - It's well formed then (type *and* name)...so use it
            newItem.sName = sData.substring(sData.lastIndexOf(" ")).trim();
            sData = sData.substring(0, sData.lastIndexOf(" ")).trim();

            newItem.sType = extractReturnType(sData);
        }

        return newItem;
    }
    
    //**************************************************************************
    // Routine: DecodeMethod
    // Outline: Given a set of object data containing a method
    //			definition, this routine extracts the method name, return type,
    //			parameter list and whether it's a constructor or not
    //**************************************************************************
    private ItemData decodeMethod(ItemData newItem)
        {
            String sChunkData = newItem.sChunkData;
            sChunkData = sChunkData.substring(0, sChunkData.indexOf("(")).trim();
            newItem.sName = sChunkData.substring(sChunkData.lastIndexOf(" ")).trim();
            sChunkData = sChunkData.substring(0, sChunkData.lastIndexOf(" ")).trim();
            newItem.sType = extractReturnType(sChunkData);
            if (newItem.sType == "[CONSTRUCTOR]")
            {
                newItem.sType = "";
                newItem.MethodType = 1;
            }
            if (newItem.sVisibility.toUpperCase().trim() == "WEBSERVICE")
            {
                newItem.MethodType = 2;
            }
            sChunkData = newItem.sChunkData;
            sChunkData = sChunkData.substring(sChunkData.indexOf("(") + 1).trim();
            sChunkData = sChunkData.substring(0, sChunkData.indexOf(")")).trim();
            newItem.sParams = sChunkData;
            return newItem;
        }
    
    //**************************************************************************
    // Routine: DecodeUnknown
    // Outline: Given a set of object data containing an 'unknown', this routine
    //			does, err, nothing
    //**************************************************************************
    private ItemData decodeUnknown(ItemData newItem) {
        return newItem;
    }
    
    //**************************************************************************
    // Method: DecodeComment
    // Given a set of object data containing a set of
    // comment data, this routine re-formats the comment into a single 
    // line (if it spanned multiple lines) and extracts any 'DOC' data
    //**************************************************************************    
    private ItemData decodeComment(ItemData newItem) {
        int length = 0;
        
        //Replace any tabs with single spaces
        String str = newItem.sChunkData.replace("\t", " ").trim();

        //If this is a single line comment...remove the leading '//'                
        if (str.startsWith("//"))
        {
            str = str.substring(2);
        } else {
            //...otherwise remove the multi-line characters
            str = str.replace("/*", "");
            str = str.replace("* ", "");
            str = str.replace("*/", "");
        }

        //Remove multiple spaces
        while (length != str.length()) {
            length = str.length();
            str = str.replace("  ", " ");
        }
        
        //Work out if the comment contains any documentation flags...and extract them if it does
        if (str.toUpperCase().contains("@DOC ")) {
            str = str.substring(str.toUpperCase().indexOf("@DOC ") + ("@".length() + 4)).trim();
            newItem.sName = str.substring(0, str.indexOf(" ")).trim();
            newItem.sParams = str.substring(str.indexOf(" ")).trim();
            return newItem;
        } else {
            newItem.sName = ""; //This *isn't* a set of 'doc' data...so forget about it
        }
        return newItem;
    }

    //**************************************************************************
    // Routine: DecodeProperty
    // Outline: Given a set of object data containing a property 
    // definition, this routine extracts the property name and type
    //**************************************************************************
    private ItemData decodeProperty(ItemData newItem) {
        String sData = newItem.sChunkData;

        //Extract the name
        sData = sData.substring(0, sData.indexOf("{")).trim();
        newItem.sName = sData.substring(sData.lastIndexOf(" ")).trim();

        //Extract the return type
        sData = sData.substring(0, sData.lastIndexOf(" ")).trim();
        newItem.sType = extractReturnType(sData);

        return newItem;
    }

    //**************************************************************************
    // Method: DecodeTest
    // Given a set of object data containing a test method, 
    // this routine extracts everything that it can about the test (by 
    // treating it as a regular method) 
    //**************************************************************************
    private ItemData decodeTest(ItemData newItem) {
        newItem = decodeMethod(newItem);

        return newItem;
    }

    //**************************************************************************
    // Method: DecodeInterface
    // Given a set of object data containing an interface 
    // definition, this routine extracts the interface name and type
    //**************************************************************************
    private ItemData decodeInterface(ItemData newItem) {
        String sChunkData = newItem.sChunkData;
        sChunkData = sChunkData.substring(sChunkData.toUpperCase().indexOf("INTERFACE ")).trim();
        sChunkData = sChunkData.substring(sChunkData.indexOf(" ")).trim();
        if (sChunkData.contains("{")) {
            sChunkData = sChunkData.substring(0, sChunkData.indexOf("{")).trim();
        }
        newItem.sName = sChunkData;
        return newItem;
    }
    
    //**************************************************************************
    // Method preProcessComments
    // 
    // 
    // 
    //**************************************************************************
    private String preProcessComments(String data, boolean removeComments) {
        int startIndex;
        int length;
        String str;
        String str2;
        String str3;

        for (startIndex = 0; data.indexOf("//", startIndex) != -1; startIndex = str.length() + str3.length()) {
            length = data.indexOf("//", startIndex);
            str = data.substring(0, length);
            if (data.indexOf("\n", data.indexOf("//")) != -1) {
                str3 = data.substring(length);
                str3 = str3.substring(0, str3.indexOf("\n"));
                str2 = data.substring(data.indexOf("\n", length));
            } else {
                str2 = "";
                str3 = data.substring(length);
            }
            str3 = str3.replace("{", "(").replace("}", ")").replace(";", " ").replace("//*", "// *");
            if (removeComments) {
                str3 = "";
            }
            data = str + str3 + str2;
        }
        for (startIndex = 0; data.indexOf("/*", startIndex) != -1; startIndex = str.length() + str3.length()) {
            length = data.indexOf("/*", startIndex);
            str = data.substring(0, length);
            if (data.indexOf("*/", length) != -1) {
                str3 = data.substring(length);
                str3 = str3.substring(0, str3.indexOf("*/"));
                str2 = data.substring(data.indexOf("*/", length));
            } else {
                str2 = "";
                str3 = data.substring(length);
            }
            str3 = str3.replace("{", "(").replace("}", ")").replace(";", " ");
            if (removeComments) {
                str3 = "";
            }
            data = str + str3 + str2;
        }
        return data;
    }
    
    //**************************************************************************
    // Method: ExtractItemName
    // Given a set of data and a piece of text to search for, this 
    // routine attempts to work out the name of the data (e.g. method
    // name, parameter name)
    //**************************************************************************
    private String extractItemName(ItemData newItem, String sFind) {
        String sData = newItem.sChunkData;

        //Does the data contain the string that we're looking for?
        if (!sData.toUpperCase().contains(sFind.toUpperCase()))
        {
            return "";
        }
        //Extract the data *after* the text that we're looking for
        sData = sData.substring(sData.toUpperCase().indexOf(sFind.toUpperCase().trim() + " ") + (sFind.trim().length() + 1)).trim();

        if (sData.contains(" "))
        {
            //Yes - Get everything up to the first space
            sData = sData.substring(0, sData.indexOf(" ")).trim();
        } else {
            //No - Check for a '{'...and use everything up to the '{'
            if (sData.contains("{")) {
                sData = sData.substring(0, sData.indexOf("{") - 1);
            }
        }
        return sData;
    }
    
    //**************************************************************************
    // Method: ExtractText
    // Given a text string, this routine returns the section of text
    // up to the supplied position. As well as this it also *removes*
    // that section of text from the original text
    //**************************************************************************
    private String extractText(RefObject<String> sData, int iToPos) {
        String sResult = "";

        //Is the position past the end of the source text?
        if (iToPos >= sData.argvalue.length())
        {
            sResult = sData.argvalue;
            sData.argvalue = "";
        } else {
            //No - Return only the section of text we were asked for
            if (iToPos >= 0) {
                //Extract the text...
                sResult = sData.argvalue.substring(0, iToPos + 1).trim();
                
                //...and remove it form the source text
                sData.argvalue = sData.argvalue.substring(iToPos + 1).trim();
            }
        }

        return sResult;
    }

    //**************************************************************************
    // Method: extractReturnType
    // Given a text string (containing something like a method or list
    // definition) this routine works out the return type  
    //**************************************************************************
    private String extractReturnType(String data)
        {
            if (data.endsWith(">"))
            {
                String str = data.substring(data.indexOf("<")).trim();
                data = data.substring(0, data.indexOf("<")).trim();

                if (data.contains(" "))
                {
                    str = data.substring(data.lastIndexOf(" ")).trim() + str;
                }
                else
                {
                    str = data.trim() + str;
                }
                return str.replace(" ", "").replace(",", ", ");
            }
            if (data.contains(" "))
            {
                return data.substring(data.lastIndexOf(" ")).trim();
            }
            return "[CONSTRUCTOR]";
        }
    
    //**************************************************************************
    // Routine: TextCount
    // Given a text string and a pattern to find this routine returns
    // the number of instances of the pattern in the text
    //**************************************************************************
    private int textCount(String sText, String sPattern) {
        int iCount = 0;
        int iIndex = 0;

         //Loop until we can't find any more instances of the pattern...
        while ((iIndex = sText.indexOf(sPattern, iIndex)) != -1){
            iIndex += sPattern.length();
            iCount += 1;
        }
        //All done, return how many instances we found
        return iCount;
    }

    //**************************************************************************
    // Class:   RefObject
    // Outline: Class used to simulate the ability to pass arguments by 
    //			reference in Java.
    //**************************************************************************
    private final class RefObject<T> {

        public T argvalue;

        public RefObject(T refarg) {
            argvalue = refarg;
        }
    }

    private class FilenameFilterImpl implements FilenameFilter {

        public FilenameFilterImpl() {
        }

        @Override
        public boolean accept(File dir, String name) {
            String lowercaseName = name.toLowerCase();
            //if (lowercaseName.startsWith("codaapi") && lowercaseName.endsWith(".cls"))
            return lowercaseName.startsWith(_configModel.FileFilter.toLowerCase()) && lowercaseName.endsWith(".cls");
        }
    }
}
