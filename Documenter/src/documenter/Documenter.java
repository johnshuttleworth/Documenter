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

    private static final String NewLine = "\n";

    private static final String AppName = "FinancialForce.com Documentation Generator";
    private static final String AppVersion = "v12.0 (16/08/2014)";
    private static final String AppCopyright = "FinancialForce.com © 2014";

    private boolean globalAbort = false;
    private String globalAbortText = "";

    private ConfigModel _configModel;
    public String sFilename = "";
    public String sChunkData = "";
    public String sChunkType = "";
    private java.util.ArrayList<ItemData> alItems = new java.util.ArrayList<ItemData>();
    private java.util.ArrayList<ItemUsage> alItemUsage = new java.util.ArrayList<ItemUsage>();
    private Map<String, String> snippetText = new HashMap<String, String>();

       private static final String ctVar = "Variables";
    
    private static final String ctUnknown = "Unknown";
    private static final String ctTest = "Tests";
    private static final String ctMethod = "Methods";
    private static final String ctWebServices = "Web Services";
    private static final String ctInterfaces = "Interfaces";
    private static final String ctClass = "Classes/Types";
    private static final String ctEnum = "Enumerations";
    
    private String IndexFolder = "Content";
    private String TocFolder = "Projects\\Tocs";
    
    private String EnumsFolder = "Content\\Reference\\Enums";
    private String ServicesFolder = "Content\\Reference\\Services";
    private String TypesFolder = "Content\\Reference\\Types";
    private String TestsFolder = "Content\\Reference\\Tests";
    private String UnknownFolder = "Content\\Reference\\Unknown";
    private String InterfacesFolder = "Content\\Reference\\Interfaces";
    
    private String SnippetsFolder = "Content\\Resources\\Snippets";
    
    private static final String ctComment = "Comment";
    private static final String ctProperty = "Property";
    
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
            DebugOut( String.format("ABORTED: %s", globalAbortText));
            return;
        }
        processSelectedFiles(latestVersionFileList);
        
        GenerateOutputDocs(TypesFolder);
        generateFiles();
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
        String sFilename = "";

        List<FilenameVersionFf> latestVersionFileList = new ArrayList<FilenameVersionFf>();
        
        alItems.clear();

        populateSnippetText();

        if (!_configModel.SourceFolder.endsWith("\\")) //Does our input folder end with a '\'?
        {
            _configModel.SourceFolder += "\\";
        }
        //Get a list of files in the input folder
        File f = new File(_configModel.SourceFolder);

        FilenameFilter textFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                //if (lowercaseName.startsWith("codaapi") && lowercaseName.endsWith(".cls")) 
                if (lowercaseName.startsWith(_configModel.FileFilter.toLowerCase()) && lowercaseName.endsWith(".cls")) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        File[] listOfFiles = f.listFiles(textFilter);

        //Do we have any files?
        if (listOfFiles == null)
        {
            System.out.println("Invalid source path");
        } else {
            for (int i = 0; i < listOfFiles.length; i++) //Iterate through the file list...
                        {
                //Is this item a file?
                if (listOfFiles[i].isFile()) {
                    sFilename = listOfFiles[i].getName();

                    String[] filenameSplitStrings = sFilename.split("_");

                    int version = 1;

                    // This happens when there is version information in the file name
                    if (filenameSplitStrings.length > 2) {
                        version = Integer.parseInt(filenameSplitStrings[1]);
                    } else {
                        // Get the class name
                        int pos = sFilename.indexOf('.');
                        filenameSplitStrings[0] = sFilename.substring(0, pos);
                    }

                    FilenameVersionFf filenameVersionFf = new FilenameVersionFf();
                    filenameVersionFf.Name = filenameSplitStrings[0];
                    filenameVersionFf.FullName = listOfFiles[i].getPath();
                    filenameVersionFf.Version = version;

                    int result = GetIndexOf(latestVersionFileList, filenameVersionFf);

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
        String chunk = "";
        String check = "";
        String chunkType = "";
        String debugText = "";
        String oldData = "";
        Integer repeatCount = 0;
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

        //Loop until we run out of data (or get told to abort)...
        while (!fileData.trim().equals("") && !bAbort && !globalAbort) {
            //To stop ourselves form getting stuck in loops, we'll keep a track
            //of the data to see if it changes across iterations. If it *doesn't*
            //then we can spot it and do something about it
            oldData = fileData;
            //Build a debug 'chain' (which should help us to figure out where we got stuck) 
            debugText += " 0->";

            //Extract the next 'chunk' of data
            RefObject<String> tempRef_sData = new RefObject<String>(fileData);
            RefObject<String> tempRef_sChunkType = new RefObject<String>(chunkType);

            //Get the next 'chunk'
            chunk = getNextChunk(tempRef_sData, tempRef_sChunkType);
            fileData = tempRef_sData.argvalue;
            chunkType = tempRef_sChunkType.argvalue;

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
                    ProcessChunk(chunk, "", filename);
                } else {
                    //No - Just output/save it
                    OutputChunk(chunkType, chunk, "", filename);

                    if (false == chunkType.equals(ctComment)) {
                        String message = String.format("%s %s we are not checking for not marked as with sharing", chunkType, filename);
                        DebugOut(message);
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

        (new java.io.File(sFolder)).mkdir(); //Create the root output folder

        if (!sFolder.endsWith("\\")) //Does our output folder end with a '\'?
        {
            sFolder = sFolder + "\\"; //Nope - Add one on
        }
        //Create the individual item type output folders
        (new java.io.File(sFolder + EnumsFolder)).mkdirs();
        (new java.io.File(sFolder + InterfacesFolder)).mkdirs();
        (new java.io.File(sFolder + ServicesFolder)).mkdirs();
        (new java.io.File(sFolder + TestsFolder)).mkdirs();
        (new java.io.File(sFolder + TypesFolder)).mkdirs();
        (new java.io.File(sFolder + UnknownFolder)).mkdirs();
        (new java.io.File(sFolder + TocFolder)).mkdirs();
        (new java.io.File(sFolder + SnippetsFolder)).mkdirs();
        System.out.println("Done");
    }
    
    private void GenerateOutputDocs(String OutputFolder) {

    }

    private void generateFiles() {

    }

    private void dumpFiles(List<FilenameVersionFf> latestVersionFileList) {
        String filesProcessedFileName = _configModel.OutputFolder + "\\FilesProcessed.txt";

        //Does the output file already exist?
        if ((new java.io.File(filesProcessedFileName)).isFile()) {
            (new java.io.File(filesProcessedFileName)).delete();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filesProcessedFileName));

            for (FilenameVersionFf file : latestVersionFileList) {
                writer.write(file.FullName + "\n");
            }

            writer.close();
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

    private void DebugOut(String message) {
        String debugFileName = _configModel.OutputFolder + "\\Debug.txt";

        try {
            Date date = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ss a");

            File file = new File(debugFileName);
            // creates the file

            if (file.exists() == false) {
                file.createNewFile();
            }

            // creates a FileWriter Object
            FileWriter writer = new FileWriter(file, true);
            // Writes the content to the file
            writer.write(ft.format(date) + "  " + message + NewLine);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        private int GetIndexOf(List<FilenameVersionFf> latestVersionFileList, FilenameVersionFf filenameVersionFf) {
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

    private void ProcessChunk(String sChunk, String string, String sFilename) {
        
    }

    private void OutputChunk(String sChunkType, String sChunk, String string, String sFilename) {
        
    }

    private String getNextChunk(RefObject<String> tempRef_sData, RefObject<String> tempRef_sChunkType) {
        return "";
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
}
