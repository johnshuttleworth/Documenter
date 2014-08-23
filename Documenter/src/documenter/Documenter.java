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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author John
 */
public class Documenter {

    
    private Integer outputtedChunks = 0;
    private Integer processedChunks = 0;
    private Integer outputChunkEnum = 0;
    private Integer outputChunkProperty = 0;
    private Integer excludedFiles = 0;
    private Integer createDocsEnums = 0;
    private Integer createdSnippetFiles = 0;
    
    private static final String NewLine = "\n";

    private static final String AppName = "FinancialForce.com Documentation Generator";
    private static final String AppVersion = "v12.0 (16/08/2014)";
    private static final String AppCopyright = "FinancialForce.com © 2014";

    private boolean globalAbort = false;
    private String globalAbortText = "";

    private ConfigModel configModel;

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
        String configFilename = "C:\\Dev_GitHub\\Documenter\\src\\ConfigFilesEtc\\V12.dcf";

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
        createOutputStructure(configModel.OutputFolder);
        
        List<FilenameVersionFf> latestVersionFileList = scanFiles();
        if(latestVersionFileList.isEmpty()){
            globalAbort = true;
            globalAbortText = "Could not find any files to process";
            debugOut( String.format("ABORTED: %s", globalAbortText));
            return;
        }
        processSelectedFiles(latestVersionFileList);
        
        if( (processedChunks == 254 || outputtedChunks == 3813) == false){
            globalAbort = true;
            globalAbortText = "Processed file count is wrong";
            debugOut( String.format("ABORTED: %s", globalAbortText));
            return;
        }
        generateFiles();
        
        String message = String.format("AllItems: %s Chunks: Processed %s Outputted %s OutputChunkEnum %s ExcludedFiles: %d", alItems.size(), processedChunks, outputtedChunks, outputChunkEnum, excludedFiles);
        debugOut(message);
        System.out.printf(message + NewLine);
        
        message = String.format("CreateDocsEnums: %d CreatedSnippetFiles %d OutputChunkProperty %d", createDocsEnums, createdSnippetFiles, outputChunkProperty);
        debugOut(message);
        System.out.printf(message + NewLine);
    }

    //**************************************************************************
    // Method: loadConfiguration
    // 
    //
    //**************************************************************************
    private void loadConfiguration(String configurationFileName) {
        configModel = ConfigurationSvc.LoadConfigFile(configurationFileName);
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

        if (!configModel.SourceFolder.endsWith("\\")) //Does our input folder end with a '\'?
        {
            configModel.SourceFolder += "\\";
        }
        //Get a list of files in the input folder
        File f = new File(configModel.SourceFolder);

        FilenameFilter textFilter = new FilenameFilterImpl();

        File[] listOfFiles = f.listFiles(textFilter);

        //Do we have any files?
        if (listOfFiles == null)
        {
            String message = String.format("****** Invalid source path: %s *******", configModel.SourceFolder);
            debugOut(message);
            System.out.println(message);
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
        
        List<String> excludeList = configModel.AllExcludeFiles;
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
                        excludedFiles++;
                        System.out.printf(fileName.FullName + " not scaned " + excludedFiles);
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
        filename = filename.substring(configModel.SourceFolder.length());
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
                        String message = String.format("WARNING: %s %s we are not checking for not marked as with sharing", chunkType, filename);
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
            sFolder += "\\"; //Nope - Add one on
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
    
    //**************************************************************************
    // Method: generateFiles
    // 
    //
    //**************************************************************************
    private void generateFiles() {
        if (alItems.isEmpty())
            {
                debugOut("There is no class data to use in the generation process. Please check your input paramaters and scan classes again.");
            }
            else
            {
                configModel.AlHtml.add(configModel.HtmlPreEnumTable);
//                configModel.HtmlPreEnumTable = FormatPlaceholders(configModel.AlHtml[0].toString(), null);

                if ((!"".equals(configModel.Namespace.trim())) && !configModel.Namespace.endsWith("."))
                {
                    configModel.Namespace += ".";
                }
                                
                generateOutputDocs(configModel.OutputFolder);
            }
    }

    //**************************************************************************
    // Method: generateOutputDocs
    // Given an output folder, this routine checks which type(s) of items we want
    // to create documents for...and then creates them
    //
    //**************************************************************************
    private void generateOutputDocs(String outputFolder) {
        ArrayList<String> tocList = new ArrayList<String>();

        if (!outputFolder.endsWith("\\")) //Does our output folder end with a '\'?
        {
            outputFolder += "\\"; 		//Nope - Add one on
        }

        alItemUsage.clear();

        if (configModel.IncludeUnknown) {
            CreateDocs(outputFolder + UnknownFolder, outputFolder, ctUnknown, configModel.UnknownTemplateFolder, tocList, true);
        }
        if (configModel.IncludeTests) {
            CreateDocs(outputFolder + TestsFolder, outputFolder, ctTest, configModel.TestTemplateFolder, tocList, true);
        }
        if (configModel.IncludeMethods) {
            CreateDocs(outputFolder + ServicesFolder, outputFolder, ctMethod, configModel.MethodsTemplateFolder, tocList, true);
        }
        if (configModel.IncludeWebServices) {
            CreateDocs(outputFolder + ServicesFolder, outputFolder, ctWebServices, configModel.WebServicesTemplateFolder, tocList, true);
        }
        if (configModel.IncludeInterfaces) {
            CreateDocs(outputFolder + InterfacesFolder, outputFolder, ctInterfaces, configModel.InterfaceTemplateFolder, tocList, true);
        }
        if (configModel.IncludeClasses) {
            CreateDocs("", outputFolder, ctClass, configModel.ClassTemplateFolder, tocList, false);
        }
        if (configModel.IncludeClasses) {
            CreateDocs(outputFolder + TypesFolder, outputFolder, ctClass, configModel.ClassTemplateFolder, tocList, true);
        }
        if (configModel.IncludeEnums) {
            CreateDocs(outputFolder + EnumsFolder, outputFolder, ctEnum, configModel.EnumTemplateFolder, tocList, true);
        }
        if ((configModel.Toc == 1) || (configModel.Toc == 3)) {
            GenerateHtmltoc(tocList, String.format("%s%s", outputFolder, IndexFolder));
        }
        if ((configModel.Toc == 2) || (configModel.Toc == 3)) {
            GenerateFlareToc(tocList, String.format("%s%s", outputFolder, TocFolder));
        }
    }
    
    //**************************************************************************
    // Method: CreateDocs
    // When called, this routine reads in the specified template file
    // (if it exists) and then attempts to use that file's data to
    // create a file for each thing of the type specified (e.g. method,
    // type). To do this it uses the placeholders in the template file
    // to insert values from the thing into HTML text...and then saves
    // the resultant text to an output file
    //**************************************************************************
    private void CreateDocs(String docPath, String rootPath, String docType, String docTemplate, List<String> tocList, Boolean createFiles) {
        String path = "";
        String sData = "";
        ArrayList<String> alDocPlaceholders = new ArrayList<String>();
        docTemplate = docTemplate.trim();
        if (docTemplate != "") {
                //System.out.printf("Creating output files: %s...", sType);

            //Load the template data
            BufferedReader inTemplate;
            try {
                inTemplate = new BufferedReader(new FileReader(docTemplate));

                String line;
                while ((line = inTemplate.readLine()) != null) {
                    sData = sData + line + "\n";
                }

                inTemplate.close();
            } catch (FileNotFoundException e) {
                globalAbortText = "FileNotFound (Template): " + docTemplate + " Error: " + e.getMessage();
                globalAbort = true;
            } catch (IOException e) {
                globalAbortText = "IOException (Template): " + docTemplate + " Error: " + e.getMessage();
                globalAbort = true;
            }

            if (globalAbort) {
                return;
            }
            sData = FormatPlaceholders(sData, alDocPlaceholders);

            if (!docPath.endsWith("\\")) {
                docPath += "\\";
            }

            if (!rootPath.endsWith("\\")) {
                rootPath += "\\";
            }

            for (ItemData itemData : alItems) {
                // These files are covered under web services so don't copy them to the types folder
                if (itemData.sOutputFile.startsWith("CODA")) {
                    //System.Diagnostics.Debug.WriteLine(objItem.sOutputFile);
                    continue;
                }

                // This is for debugging
                if ((itemData.sChunkType == null ? docType == null : itemData.sChunkType.equals(docType)) && (docType == null ? ctEnum == null : docType.equals(ctEnum))) {
                    createDocsEnums++;
                }

                if (((itemData.sChunkType == null ? docType == null : itemData.sChunkType.equals(docType)) &&
                        (((!"".equals(itemData.sParentClass.trim())) || !configModel.SkipRootClasses) || (!itemData.sChunkType.equals(ctClass)))) &&
                        (((!itemData.sChunkType.equals(ctMethod)) || (itemData.MethodType != 1)) || !configModel.SkipConstructor)) {
                    
                    //Are we creating files?
                    if (createFiles) {
                        path = docPath + itemData.sOutputFile;

                        //Does the output file already exist?
                        if ((new File(path)).isFile()) {
                            //Yes - Work out what to do. Are we overwriting?
                            if (!configModel.bOverwriteDocs) {
                                //No - Create a backup of the file
                                String sBackupFile = DateToShortDateString(new Date()) + " " + DateToShortTimeString(new Date());
                                sBackupFile = sBackupFile.replace("\\", "-").replace(":", "-").replace("/", "").trim();

                                sBackupFile = path + itemData.sName + Integer.toString(itemData.iInstance) + "-" + sBackupFile + DefFileExt;

                                try {
                                    copyFile(path, sBackupFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            (new java.io.File(path)).delete();
                        }
                    }
                    String source = itemData.sOutputFile;
                    if (source.contains(".")) {
                        source = source.substring(0, source.indexOf('.')).trim();
                    }
                    String sOutputMethodName = GenerateSnippetFilename(itemData);

                        //if (itemData.sName.ToUpper() == "HEADERDETAILS")
                    //{
                    //    sName = itemData.sName;
                    //}
                    String sName = sData;
                    String sUsedByName = itemData.sName;
                    if (!"".equals(itemData.sParentClass.trim())) {
                        sUsedByName = itemData.sParentClass + "." + sUsedByName;
                    }
                    sUsedByName = configModel.Namespace + sUsedByName;
                    configModel.SnippetsList.clear();

                    sName = sName.replace("[%FULLNAME%]", sUsedByName);
                    sName = sName.replace("[%NAME%]", itemData.sName);
                    sName = sName.replace("[%ITEMTYPE%]", itemData.sChunkType);
                    sName = sName.replace("[%FILENAME%]", itemData.sSourceFile);
                    sName = sName.replace("[%INPUTPARAMS%]", createParamsData(sOutputMethodName, sUsedByName, itemData.iInstance, itemData.sChunkType, itemData.sParams, itemData.sParentClass, "", "", false));
                    sName = sName.replace("[%INPUTPARAMSCR%]", createParamsData(sOutputMethodName, sUsedByName, itemData.iInstance, itemData.sChunkType, itemData.sParams, itemData.sParentClass, NewLine, configModel.ParamIndent, false));
                    sName = sName.replace("[%INPUTPARAMSTABLE%]", createParamsData(sOutputMethodName, sUsedByName, itemData.iInstance, itemData.sChunkType, itemData.sParams, itemData.sParentClass, "", "", true));
                    sName = sName.replace("[%PARENT%]", itemData.sParentClass).replace("[%RETURNTYPE%]", buildReturnLink(itemData.MethodType, sUsedByName, itemData.iInstance, itemData.sChunkType, itemData.sType, itemData.sParentClass, false));
                    sName = sName.replace("[%RETURNTYPETEXT%]", buildReturnLink(itemData.MethodType, sUsedByName, itemData.iInstance, itemData.sChunkType, itemData.sType, itemData.sParentClass, true));
                    sName = sName.replace("[%VALUES%]", itemData.sValues).replace("[%VALUESTABLE%]", CreateValuesTable(sOutputMethodName, itemData.sValues));
                    sName = sName.replace("[%USAGELIST%]", createUsageList(itemData.sName, itemData.sParentClass));
                    sName = sName.replace("[%INTERFACELIST%]", createInterfaceList(itemData.sName, itemData.iInstance, itemData.sChunkType, itemData.sChunkData, itemData.sParentClass));
                    sName = sName.replace("[%PROPERTYLIST%]", createPropertyList(sOutputMethodName, itemData.sName, itemData.sParentClass, itemData.sExtends, itemData.sChunkType, itemData.iInstance));
                    sName = sName.replace("[%VISIBILITY%]", itemData.sVisibility);
                    sName = sName.replace("[%RAWDATA%]", itemData.sChunkData);
                    sName = sName.replace("[%PRODUCTNAMESPACE%]", configModel.Namespace);
                    sName = sName.replace("[%OUTPUTNAME%]", source);
                    sName = sName.replace("[%OUTPUTFILENAME%]", itemData.sOutputFile);
                    sName = sName.replace("[%OUTPUTPATH%]", docPath);
                    sName = sName.replace("[%DATE%]", DateToShortTimeString(new Date()));
                    sName = sName.replace("[%TIME%]", DateToShortTimeString(new Date()));

                    String str10 = sName;
                    sName = sName.replace("[%SNIPPETDESCR%]", sOutputMethodName + "-descr.flsnp");
                    Boolean bSnippetDescr = (str10.equals(sName) == false);
                    str10 = sName;
                    sName = sName.replace("[%SNIPPETEXAMPLE%]", sOutputMethodName + "-example.flsnp");
                    Boolean bSnippetExample = (str10.equals(sName) == false);
                    str10 = sName;
                    sName = sName.replace("[%SNIPPETINPUT%]", sOutputMethodName + "-input.flsnp");
                    Boolean bSnippetInput = (str10.equals(sName) == false);
                    str10 = sName;
                    sName = sName.replace("[%SNIPPETOUTPUT%]", sOutputMethodName + "-output.flsnp");
                    Boolean bSnippetOutput = (str10.equals(sName) == false);

                    for (String placeHolder : alDocPlaceholders) {
                        String sPlaceholder = placeHolder;
                        String newValue = getDocData(sPlaceholder, itemData.sName, itemData.sParentClass);
                        sName = sName.replace(sPlaceholder, newValue);
                    }

//                        StatusMsg = "Generating:";
                    if (createFiles) {
                            //Yes - Create the file (and create an entry in the TOC if we have to)
                        //We have the data for this file...so save it
                        BufferedWriter writer = null;
                        try {
                             // This is for debugging
//                if ((itemData.sChunkType == docType) && docType == ctEnum) {
//                    debugOut("Enum writing:  " + path);
//                }
                            writer = new BufferedWriter(new FileWriter(path));
                            writer.write(sName);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (configModel.Toc != 0) {
                            tocList.add(itemData.sChunkType + "[" + sUsedByName + "]" + path);
                        }
                        if (configModel.IncludeSnippets) {
                            createSnippetFiles(String.format("%s%s\\%s", rootPath, SnippetsFolder, sOutputMethodName), configModel.SnippetsList, bSnippetDescr, bSnippetExample, bSnippetInput, bSnippetOutput, configModel.SnippetMarker);
                        }
                    }
                }
            }
        }
    }
    
    private void dumpFiles(List<FilenameVersionFf> latestVersionFileList) {
        String filesProcessedFileName = configModel.OutputFolder + "\\FilesProcessed.txt";

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
        String debugFileName = configModel.OutputFolder + "\\Debug.txt";

        try {
            Date date = new Date();
            String dateTime = DateToShortDateTimeString(new Date());

            File file = new File(debugFileName);
            // creates the file

            if (file.exists() == false) {
                file.createNewFile();
            }

            // Writes the content to the file
            try (
                    // creates a FileWriter Object
                    FileWriter writer = new FileWriter(file, true)) {

                String debugMessage = String.format("%s ********* %s%s%s *********", dateTime, NewLine, message, NewLine);
                // Writes the content to the file
                writer.write(debugMessage);
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

        // This is for debugging
        String savedData = data.argvalue;
        
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
                            //chunkType.argvalue = ctProperty;
                            chunkType.argvalue = data.argvalue.startsWith("enum ") ? "Enumerations" : "Property";
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
                        if (("".equals(data.argvalue)) && (iIndent != 0))
                                {
                                    data.argvalue = data.argvalue.trim();
//                                    str4 = str4.Trim();
                                    break;
                                }
                    } while (iIndent > 0);
                    break;
            }

            //Check to see if this thing is an enum...and set it as such (if it is)
            if ((chunkType.argvalue.equals(ctClass))
                    && (!result.contains(";") && (result.toLowerCase().contains(" enum ")))) {
                chunkType.argvalue = ctEnum;
                
                //debugOut(savedData);
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
        String nextChunk = "";
        String chunkType = "";
        String sCheck = "";
        String sClass = "";

        while (data.trim() != "") {
            //Extract the next 'chunk' of data
            RefObject<String> tempRef_sData = new RefObject<String>(data);
            RefObject<String> tempRef_sChunkType = new RefObject<String>(chunkType);

            //Get the next 'chunk'
            nextChunk = getNextChunk(tempRef_sData, tempRef_sChunkType);
            data = tempRef_sData.argvalue;
            chunkType = tempRef_sChunkType.argvalue;

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
                    if ("".equals(parentClass.trim())) {
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
        
        if (!"".equals(sChunk.trim()))
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

                if ((((str.equals("PUBLIC")) && configModel.ShowPublic) || ((str.equals("PRIVATE")) && configModel.ShowPrivate)) 
                    || ((((str.equals("GLOBAL")) && configModel.ShowGlobal) || ((str.equals("WEBSERVICE")) && configModel.ShowWebService)) || (str.equals("N/A"))))
                {
                    ItemData newItem = new ItemData();
                    newItem.sName = "";
                    newItem.sParentClass = sParentClass;
                    newItem.sChunkType = sChunkType;
                    newItem.sChunkData = sChunk;
                    newItem.sVisibility = str;
                    newItem.sSourceFile = sFilename;
                    newItem.iInstance = 1;

if(sFilename.equals("CODAAPIBankStatement_6_0.cls") && sChunkType.equals(ctWebServices))
    debugOut("");
                    switch (sChunkType)
                    {
                        case ctClass:
                            newItem = decodeClass(newItem);
                            break;

                        case ctEnum:
                            outputChunkEnum++;
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
                            outputChunkProperty++;
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
                            
                        default:
                            String message = String.format("ERROR: This item was not outputted *********** %s", sChunkType);
                        debugOut(message);
                            break;
                    }
                    
                    Boolean debugPrint = false;
                    if (newItem.sName.startsWith("enumCreditNoteStatus"))
                    {
                        //debugOut("DEBUG: " + newItem.sName);
                        debugPrint = true;
                    }
                     
                    //Did we identify a name for this chunk?
                    if (!"".equals(newItem.sName.trim()))
                    {
                        for (ItemData data2 : alItems)
                        {
//                            if (data2.sName.startsWith("enumCreditNoteStatus"))
//                            {
//                                debugOut("DEBUG: " + data2.sName);
//                            }
//
//                            if ("Enumerations".equals(newItem.sChunkType) && (data2.sChunkType == null ? ctEnum == null : data2.sChunkType.equals(ctEnum)))
//                            {
//                                if(data2.sName.toUpperCase().trim() == null ? newItem.sName.toUpperCase().trim() == null : data2.sName.toUpperCase().trim().equals(newItem.sName.toUpperCase().trim()))
//                                {
//                                    if (newItem.sName.startsWith("enumCreditNoteStatus"))
//                                    {
//                                        debugOut("DEBUG: " + data2.sName);
//                                    }                                    
//                                }
//                            }
                            if (((data2.sChunkType == null ? newItem.sChunkType == null : data2.sChunkType.equals(newItem.sChunkType)) && (data2.sName.toUpperCase().trim() == null ? newItem.sName.toUpperCase().trim() == null : data2.sName.toUpperCase().trim().equals(newItem.sName.toUpperCase().trim()))) && (data2.iInstance >= newItem.iInstance))
                            {
                                newItem.iInstance = data2.iInstance + 1;
                            }
                        }
                        newItem.sOutputFile = newItem.sName + newItem.iInstance + ".htm";
                        alItems.add(newItem);
//                        if(debugPrint)
//                        {
//                            debugOut("DEBUG: allItems.add   " + newItem.sOutputFile + "    " + newItem.sChunkType);
//                        }
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
    // Given a set of object data containing an 
    // enumeration, this routine extracts the enumeration name and its values
    // 
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
            if ("[CONSTRUCTOR]".equals(newItem.sType))
            {
                newItem.sType = "";
                newItem.MethodType = 1;
            }
            if ("WEBSERVICE".equals(newItem.sVisibility.toUpperCase().trim()))
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
    // Method: FormatPlaceholders
    // This routine takes the supplied text and converts any placeholders ('[%<Name>%]' to
    // uppercase (making them easier to match in the future)
    // 
    //**************************************************************************
    private String FormatPlaceholders(String sData, ArrayList<String> alDocPlaceholders) {
        String sPlaceholder = "";
        StringBuilder sbResult = new StringBuilder();

        while (sData.contains("[%")) //Loop until we run out of placeholders...
        {
            //Add everything *up to* the placeholder to the output data
            sbResult.append(sData.substring(0, sData.indexOf("[%")));

            //Remove everything *up to* the placeholder form the source data
            sData = sData.substring(sData.indexOf("[%"));

            if (sData.contains("%]")) //If the source data contains the *end* of a placeholder...
            { //...extract the placeholder
                //Extract the placeholder and convert it to uppercase
                sPlaceholder = sData.substring(0, sData.indexOf("%]") + 2).toUpperCase().trim();
                sData = sData.substring(sData.indexOf("%]") + 2);

                sbResult.append(sPlaceholder); //Add the placeholder to the output data

                if (alDocPlaceholders != null) //Are we building a list of 'DOC' placeholders?
                { 									//Yes - Add this to the list (Assuming that it's a 'DOC' placeholder of course)
                    if (sPlaceholder.startsWith("[%DOC:") && (alDocPlaceholders.indexOf(sPlaceholder) < 0)) {
                        alDocPlaceholders.add(sPlaceholder);
                    }
                }
            }
        }

        sbResult.append(sData); //Add any remaining data to the output

        return sbResult.toString();
    }

    //**************************************************************************
    // Method: GenerateHtmltoc
    // 
    // 
    // 
    //**************************************************************************
    private void GenerateHtmltoc(ArrayList<String> tocList, String outputFolder) {
        String upperCaseHeading = "";

        java.util.Collections.sort(tocList);
        
        if (!outputFolder.endsWith("\\")) {
            outputFolder += "\\";
        }

        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder + "index.htm"))) {
                writer.write("<Table>");
                
                for (String tocItem : tocList) {
                    String heading = tocItem.substring(0, tocItem.indexOf("[")).trim();
                    String fullFilename = tocItem.substring(tocItem.indexOf("]") + 1).trim();
                    String displayText = tocItem.substring(tocItem.indexOf("[") + 1).trim();
                    displayText = displayText.substring(0, displayText.indexOf("]")).trim();
                    if (!heading.toUpperCase().trim().equals(upperCaseHeading)) {
                        writer.write("<tr><td>" + heading + "</a></td><td></td></tr>");
                        upperCaseHeading = heading.toUpperCase().trim();
                    }
                    String linkText = fullFilename.substring(outputFolder.length()).trim();
                    if (!configModel.IncludeTocFullNames && displayText.contains("\\.")) {
                        displayText = displayText.substring(displayText.lastIndexOf(".") + 1).trim();
                    }
                    writer.write("<tr><td></td><td><a href=\"" + linkText + "\">" + displayText + "</a></td></tr>");
                }
                writer.write("</table>");
            }
        } catch (IOException e) {
        }
    }

    private void GenerateFlareToc(ArrayList<String> tocList, String outputFolder) {
        String upperCaseHeading = "";
            String tempSource = "";
            
            java.util.Collections.sort(tocList);

            if (!outputFolder.endsWith("\\"))
            {
                outputFolder += "\\";
            }

            try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder + "PrimaryTOC.fltoc"))) {
                
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + NewLine);
            writer.write("<CatapultToc" + NewLine);
            writer.write("  Version=\"1\">" + NewLine);
            if (configModel.TocPreNodes.trim() != "")
            {
//                writer.write(TocLinks.GenerateFlareXml(configModel.TocPreNodes).trim());
            }
            writer.write("  <TocEntry" + NewLine);
            writer.write("    Title=\"API Reference\"" + NewLine);

            writer.write("    Link=\"/Content/Reference/Reference.htm\">" + NewLine);

            for (String tocItem : tocList)
            {
                String heading = extractSubString(0, tocItem, "[", 0 ).trim();
                String fullFilename = extractSubString(tocItem, "]", 1).trim();
                String displayText = extractSubString(tocItem, "[", 1).trim();
                displayText = extractSubString(0, displayText, "]", 0).trim();

                String source;
                if (heading.toUpperCase().trim() != upperCaseHeading)
                {
                    if (upperCaseHeading.trim() != "")
                    {
                        writer.write("      </TocEntry>" + NewLine);
                        writer.write("    </TocEntry>" + NewLine);
                    }
                    source = displayText;
                    if (configModel.Namespace.trim() != "")
                    {
                        source = source.substring(configModel.Namespace.length()).trim();
                    }
                    if (source.contains("\\."))
                    {
                        source = source.substring(0, source.indexOf(".")).trim();
                    }
                    tempSource = source;
                    writer.write("    <TocEntry" + NewLine);
                    writer.write("      Title=\"" + heading + "\">" + NewLine);
                    writer.write("      <TocEntry" + NewLine);
                    writer.write("        Title=\"" + source + "\">" + NewLine);
                    upperCaseHeading = heading.toUpperCase().trim();
                }
                else
                {
                    source = displayText;
                    if (configModel.Namespace.trim() != "")
                    {
                        source = source.substring(configModel.Namespace.length()).trim();
                    }
                    if (source.contains("\\."))
                    {
                        source = source.substring(0, source.indexOf(".")).trim();
                    }
                    if (tempSource != source)
                    {
                        writer.write("      </TocEntry>" + NewLine);
                        writer.write("      <TocEntry" + NewLine);
                        writer.write("        Title=\"" + source + "\">" + NewLine);
                        tempSource = source;
                    }
                }

                //fullFilename = Path.GetFileName(fullFilename).Replace(@"\", "/").trim();
                
                // We need to do this to get the sub folder which is  type
                String pattern = Pattern.quote(System.getProperty("file.separator"));
                String[] filenameParts = fullFilename.split(pattern);
                
                String typeName = filenameParts[filenameParts.length - 2];
                String fileName = filenameParts[filenameParts.length - 1];

                String subFolderAndFilename = String.format("%s/%s", typeName, fileName);
                //string subFolderAndFilename = fullFilename.Substring()
                //fullFilename = fullFilename.Substring(folder.Length).Replace(@"\", "/").trim();
                
                if (!configModel.IncludeTocFullNames && displayText.contains("\\."))
                {
                    displayText = extractSubString(displayText, ".", 1).trim();
                }
                writer.write("        <TocEntry" + NewLine);
                writer.write("          Title=\"" + displayText + "\"" + NewLine);
                writer.write("          Link=\"/Content/Reference/" + subFolderAndFilename + "\" />" + NewLine);
                //writer.write(string.Format("          Link=\"/Content/Reference/{0}/{1} \" />", typeName, fileName));
            }
            if (upperCaseHeading.trim() != "")
            {
                writer.write("      </TocEntry>" + NewLine);
                writer.write("    </TocEntry>" + NewLine);
            }
            writer.write("  </TocEntry>" + NewLine);
            if (configModel.TocPostNodes.trim() != "")
            {
//                writer.write(TocLinks.GenerateFlareXml(configModel.TocPostNodes).trim());
            }
            writer.write("</CatapultToc>" + NewLine);
            writer.flush();
            writer.close();
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //**************************************************************************
    // Routine: copyFile
    // Outline: Given a source file and a destination file, this routine, 
    // copies from the source file to the destination file
    //**************************************************************************
    private void copyFile(String sourceFilename, String destFilename) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;

        File sourceFile = new File(sourceFilename);
        File destFile = new File(destFilename);

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }

            if (destination != null) {
                destination.close();
            }
        }
    }
   
    //**************************************************************************
    // Method: CreateValuesTable
    // Given a set of delimited text, this routine converts that text
    // into a list and then uses it to construct a HTML table (with one
    // item in the list having one row in the output table). The HTML 
    // used to construct the table is that specified in the config file
    //**************************************************************************
    private String CreateValuesTable(String sOutputMethodName, String sData) {
        String[] strArray = sData.split("\\,");
        String sHtmlPreEnumTable = configModel.HtmlPreEnumTable;
        for (String item : strArray) {
            String newValue;
            if ("".equals(item.trim())) {
                newValue = "";
            } else {
                configModel.SnippetsList.add(sOutputMethodName + "-value-" + item.trim());
                newValue = "<MadCap:snippetBlock src=\"../../Resources/Snippets/" + sOutputMethodName + "-value-" + item.trim() + ".flsnp\" />";
            }
            sHtmlPreEnumTable = sHtmlPreEnumTable + configModel.HtmlLoopEnumTable.replace("[%ENUMVALUE%]", item).replace("[%SNIPPETLINK%]", newValue);
        }
        return (sHtmlPreEnumTable + configModel.HtmlPostEnumTable);
    }

    //**************************************************************************
    // Method: GenerateSnippetFilename
    // 
    // 
    //**************************************************************************
    private String GenerateSnippetFilename(ItemData itemData) {
        String str = itemData.sParentClass + "-" + itemData.sName;
        if ((("Methods".equals(itemData.sChunkType)) || ("Tests".equals(itemData.sChunkType))) || ("Web Services".equals(itemData.sChunkType))) {
            if ("".equals(itemData.sParams.trim())) {
                return (str + "0");
            }
            String[] strArray = itemData.sParams.toUpperCase().split("\\,");
            str = String.format("%s%d", str, strArray.length);

            for (String item : strArray) {
                String tempStr
                        = item.replace(" ", ".")
                        .replace("<", ".")
                        .replace("[", ".")
                        .replace(">", "")
                        .replace("]", "")
                        .replace("DATE", "D.T")
                        .replace("SOBJECT", "S.");

                    //if (tempStr != item)
                //{
                //    Console.WriteLine("");    
                //}
                String[] strArray2 = tempStr.split("\\.");

                for (String fileName : strArray2) {
                    if (fileName.trim().equals("") == false) {
                        char firstChar = fileName.charAt(0);
                        str = String.format("%s%c", str, firstChar);
                    }
                }
            }
        }
        return str;
    }

    //**************************************************************************
    // Method: extractSubString
    // By using this method it aids porting between OS's
    //**************************************************************************
    private static String extractSubString(String data, String toFind, int offset)
    {
        return data.substring(data.indexOf(toFind) + offset).trim();
    }
    
    private static String extractSubString(int startIndex, String data, String toFind, int offSet)
    {
        return data.substring(startIndex, data.indexOf(toFind) + offSet).trim();
    }
    //**************************************************************************
    // Method: DateToShortTimeString
    // Given a date, this routine formats it as HH:mm:ss format
    //**************************************************************************
    private String DateToShortTimeString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }

    //**************************************************************************
    // Routine: DateToShortDateString
    // Outline: Given a date, this routine formats it in yyyy-MM-dd format
    //**************************************************************************
    private String DateToShortDateString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    //**************************************************************************
    // Methd: DateToShortDateTimeString
    // Given a date, this routine formats it in dd/MM/yyyy HH:mm:ss format
    //**************************************************************************
    private String DateToShortDateTimeString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return formatter.format(date);
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
            return lowercaseName.startsWith(configModel.FileFilter.toLowerCase()) && lowercaseName.endsWith(".cls");
        }
    }
    
    //**************************************************************************
    // Method: createParamsData
    // Given a set of data, this routine builds an output HTML 
    // structure detailing the parameters, types, hyperlinks (where necessary) etc.
    // 
    //**************************************************************************    
    private String createParamsData(String outputMethodName, String usedByName, int usedByInstance, String usedByChunkType, String data, String parentClass, String lineEnd, String lineStart, Boolean table) {
        Boolean flag = false;
        Boolean bError = false;
        int num = 0;
        ArrayList<String> alParams = new ArrayList<String>();
        String htmlPreParamTable = "";
        String sParam = "";
        String newValue;
        String sType;
        String str6;

        //Build the list of parameters
        List<String> list2 = new ArrayList<String>();

        //Strip out any '/*...*/' comments (it's easier to do here than later on)
        while (data.contains("/*") && !bError) {
            String sComment = data.substring(data.indexOf("/*"));

            data = data.substring(0, data.indexOf("/*"));

            if (sComment.contains("*/")) {
                sComment = sComment.substring(sComment.indexOf("*/") + 2).trim();
                data = data + sComment;
            } else {
                bError = true;
            }
        }

        //Iterate through the data working out if there are any nested parameters (e.g. List<Int, List<String>>)... 
        for (int iCount = 0; iCount < data.length(); iCount++) {
            //As parameters *could* be lists/sets/maps (and possibly *nested*) we
            //need to keep a track of how 'nested' we are
            switch (data.charAt(iCount)) {
                case '<':
                    num++;
                    sParam = sParam + "<";
                    break;

                case '>':
                    num--;
                    sParam = sParam + ">";
                    break;

                case ',':
                    //Are we inside another parameter (e.g. List<Integer, List<...)?
                    if (num == 0) {
                        list2.add(sParam.trim());
                        sParam = "";
                    } else {
                        //Yes - Keep constructing the parameter
                        sParam += ",";
                    }
                    break;
            }
            // NOT SURE ABOUT THIS
            sParam += data.charAt(iCount);
        }
        if (sParam.trim() != "") {
            list2.add(sParam);
        }

        //Check for any inline '//...' comments...and remove them
        for (int j = 0; j < list2.size(); j++) {
            sParam = list2.get(j);
            if (sParam.trim().startsWith("//") && sParam.contains("\n")) {
                sParam = sParam.substring(sParam.indexOf("\n")).trim();
                list2.set(j, sParam);
            }
        }

        String[] aParams = list2.toArray(new String[0]);

        if (aParams.length == 0) {
            flag = true;
        }
        if ((aParams.length == 1) && ("".equals(aParams[0].trim()))) {
            flag = true;
        }
        if (table) {
            if (flag) {
                return "This method has no input parameters";
            }
            htmlPreParamTable = configModel.HtmlPreParamTable;
            for (String arrayData : aParams) {
                str6 = arrayData.trim().contains(" ") ? " " : ">";

                newValue = arrayData.trim();
                newValue = newValue.substring(newValue.lastIndexOf(str6)).trim();

                configModel.SnippetsList.add(outputMethodName + "-param-" + newValue.trim());
                String str7 = "<MadCap:snippetBlock src=\"../../Resources/Snippets/" + outputMethodName + "-param-" + newValue.trim() + ".flsnp\" />";

                sType = arrayData.trim();
                sType = sType.substring(0, sType.lastIndexOf(str6)).trim();

                sType = buildTypeLink(sType, parentClass, false, false);
                htmlPreParamTable = htmlPreParamTable + configModel.HtmlLoopParamTable.replace("[%PARAMNAME%]", newValue).replace("[%PARAMTYPE%]", sType).replace("[%SNIPPETLINK%]", str7);
            }
            return (htmlPreParamTable + configModel.HtmlPostParamTable);
        }
        if (!flag) {
            for (String arrayData : aParams) {
                str6 = arrayData.trim().contains(" ") ? " " : ">";

                newValue = arrayData.trim();
                newValue = newValue.substring(newValue.lastIndexOf(str6)).trim();

                configModel.SnippetsList.add(outputMethodName + "-param-" + newValue.trim());

                sType = arrayData.trim();
                sType = sType.substring(0, sType.lastIndexOf(str6)).trim();
                sType = buildTypeLink(sType, parentClass, true, false);
                String usedByLink = buildUsedByLink(usedByName, usedByChunkType, usedByInstance);
                storeTypeLink(sType, usedByLink, usedByName, usedByChunkType);
                if ((sType + " " + newValue).trim() != "") {
                    alParams.add(sType + " " + newValue);
                }
            }
            for (int n = 0; n < alParams.size(); n++) {
                if (n != 0) {
                    htmlPreParamTable = htmlPreParamTable + ", " + lineEnd;
                }
                htmlPreParamTable = htmlPreParamTable + lineStart + alParams.get(n).toString().trim();
            }
        }
        return htmlPreParamTable;
    }
    

    //**************************************************************************
    // Routine: BuildUsedByLink
    // Outline: Given a name, chunk type and instance, this routine builds a
    //			hyperlink to the corresponding help file
    //**************************************************************************
    private String buildUsedByLink(String sName, String sChunkType, int iInstance) {
        String sLink = sName.trim();

        if (sLink.contains(".")) //If the thing is within something else...
        {
            sLink = sLink.substring(sLink.lastIndexOf(".") + 1).trim();	//...get *only* the thing  
        }
        sLink = getChunkTypePath(sChunkType) + sLink + (new Integer(iInstance)).toString() + DefFileExt;

        return sLink;
    }
    
    private String buildReturnLink(Integer methodType, String usedByName, int usedByInstance, String usedByChunkType, String returnType, String parentClass, boolean asText) {
        String data;

            String methodTypeTxt = "service";
            if (methodType == 1)
            {
                methodTypeTxt = "constructor";
            }
            if (methodType == 2)
            {
                methodTypeTxt = "web service";
            }
            if (asText)
            {
                data = buildTypeLink(returnType, parentClass, false, true);
                if (("VOID".equals(data.toUpperCase().trim())) || ("".equals(data.trim())))
                {
                    return String.format("This {0} does not return a value.", methodTypeTxt);
                }
                if (isVowel(data))
                {
                    return String.format("This {0} returns an {1}.", methodTypeTxt, data);
                }
                return String.format("This {0} returns a {1}.", methodTypeTxt, data);
            }

            data = buildTypeLink(returnType, parentClass, true, false);
            storeTypeLink(data, buildUsedByLink(usedByName, usedByChunkType, usedByInstance), usedByName, usedByChunkType);
            return data;
    }

    //**************************************************************************
    // Method: createUsageList
    // When called, this routine takes the supplied name and builds a
    // 'Used By...' HTML structure
    //**************************************************************************
    private String createUsageList(String className, String parentClass) {
        String sHtmlPreMethodList = "";

        if (alItemUsage.size() <= 0) {
            return sHtmlPreMethodList;
        }

        //Build a list containing all of the things that refer to the item we're looking for
        List<String> usageList = new ArrayList<>();

        for (ItemUsage usage : alItemUsage) {
            if ((((usage.sItemName.equals((configModel.Namespace + parentClass + "." + className)))
                    && (!"".equals(usage.sUsedByName.trim()))) 
                    && ((!"".equals(usage.sItemName.trim()))
                    && (!"".equals(usage.sUsedByType.trim())))) 
                    && (usageList.indexOf(usage.sUsedByName + "|" + usage.sFilename + "|" + usage.sUsedByType) == -1)) {
                
//We have a set of valid data so...add it to our list of items
                usageList.add(usage.sUsedByName + "|" + usage.sFilename + "|" + usage.sUsedByType);
            }
        }

        if (usageList.size() <= 0) {
            return sHtmlPreMethodList;
        }

        java.util.Collections.sort(usageList);

        sHtmlPreMethodList = configModel.HtmlPreMethodList;
        for (String preItem : usageList) {
            String newValue = preItem;
            String itemName = newValue.substring(0, newValue.indexOf("|")).trim();
            newValue = newValue.substring(newValue.indexOf("|") + 1).trim();
            String itemType = newValue.substring(newValue.indexOf("|") + 1).trim();
            String link = newValue.substring(0, newValue.indexOf("|")).trim();
            sHtmlPreMethodList = sHtmlPreMethodList + configModel.HtmlLoopMethodList.replace("[%ITEMLINK%]", link).replace("[%ITEMNAME%]", itemName).replace("[%ITEMTYPE%]", itemType);
        }
        return (sHtmlPreMethodList + configModel.HtmlPostMethodList);
    }

    //**************************************************************************
    // Method: createPropertyList
    // Given a set of class data, this routine creates a HTML table 
    // containing all of the properties for that class (formatted as
    // per the config file)
    //**************************************************************************
    private String createPropertyList(String outputMethodName, String className, String parentClass, String extendsClass, String itemChunkType, int instance) {
        String sResult = "";
            ArrayList<String> alProps = new ArrayList<String>();

            //Does this class extend another class?
            if (!"".equals(extendsClass.trim()))
            {
                String sItemName = buildTypeLink(extendsClass, parentClass, false, false);
                sResult = "This class/type extends " + sItemName + ".<BR/>";
                storeTypeLink(sItemName, buildUsedByLink(className, itemChunkType, instance), configModel.Namespace + parentClass + "." + className, itemChunkType);
            }

            // Build the list of properties for this class
            for (ItemData itemData : alItems)
            {
                // If this thing is a property (or a variable) *and* it's parent is the class
                // we're interested in...add it to our list of properties
                if ((("Property".equals(itemData.sChunkType)) || ("Variables".equals(itemData.sChunkType))) && (itemData.sParentClass.equals(parentClass + "." + className)))
                {
                    alProps.add(itemData.sName + "[" + itemData.sType + "]");
                }
            }

            //Did we find any properties for this class?
            if (alProps.size() <= 0)
            {
                return sResult;
            }
            
            java.util.Collections.sort(alProps);
            
            sResult = sResult + configModel.HtmlPrePropList;

            //Iterate through the list of properties
            for (String item : alProps)
            {
                //Get the details of the property
                String sPropertyName = item.trim();
                String sPropertyType = extractSubString(sPropertyName, "[", 1).trim();
                if (sPropertyType.endsWith("]"))
                {
                    sPropertyType = sPropertyType.substring(0, sPropertyType.length() - 1).trim();
                }
                
                sPropertyName = extractSubString(0, sPropertyName, "[", 0).trim();
                
                configModel.SnippetsList.add(outputMethodName + "-prop-" + sPropertyName.trim());
                
                //Add a hyperlink to the type (if necessary)
                String snippetLink = "<MadCap:snippetBlock src=\"../../Resources/Snippets/" + outputMethodName + "-prop-" + sPropertyName.trim() + ".flsnp\" />";
                sPropertyType = buildTypeLink(sPropertyType, parentClass, false, false);
                
                //We're referencing a type...so make sure that we remember the reference (for the 'Used By...' list)
                storeTypeLink(sPropertyType, buildUsedByLink(className, itemChunkType, instance), configModel.Namespace + parentClass + "." + className, itemChunkType);
                
                //Add the formatted name, type etc. to the result text
                sResult = sResult + configModel.HtmlLoopPropList.replace("[%PROPERTYNAME%]", sPropertyName).replace("[%PROPERTYTYPE%]", sPropertyType).replace("[%SNIPPETLINK%]", snippetLink);
            }
            return (sResult + configModel.HtmlPostPropList);
    }

    //**************************************************************************
    // Method: createInterfaceList
    // 
    // 
    //**************************************************************************
    private String createInterfaceList(String className, int instance, String chunkType, String interfaceData, String parentClass) {
            if (!"Interfaces".equals(chunkType)) {
                return "";
            }
            
            if (interfaceData.contains("{"))
            {
                interfaceData = extractSubString(interfaceData, "{", 1);
            }
            if (interfaceData.contains("}"))
            {
                interfaceData = extractSubString(0, interfaceData, "}", 0);
            }

            String sHtmlPreInterfaces = "";

            String[] strArray = interfaceData.split(";");
            if (strArray.length <= 0)
            {
                return sHtmlPreInterfaces;
            }
            
            sHtmlPreInterfaces = configModel.HtmlPreInterfaces;
            for (String preItem : strArray)
            {
                if ("".equals(preItem))
                {
                    continue;
                }

                ItemData newItem = new ItemData();
                newItem.sChunkData = preItem.trim();

                if (newItem.sChunkData.contains("("))
                {
                    newItem = decodeMethod(newItem);
                    String name = newItem.sName;
                    String paramsData = newItem.sParams;
                    paramsData = createParamsData("", className, instance, chunkType, paramsData, parentClass, "", "", false);
                    
                    String str4;
                    if ("".equals(newItem.sType.trim())) {
                        str4 = "void";
                    } else {
                        str4 = buildTypeLink(newItem.sType, className, false, false);
                    }
                    
                    String newValue = "";
                    for (ItemData item : alItems)
                    {
                        if (((item.sName.toUpperCase().trim() != name.toUpperCase().trim()) ||
                             (item.sParentClass.toUpperCase().trim() != parentClass.toUpperCase().trim())) ||
                            (item.sChunkType != "Methods")) continue;
                        newValue = getChunkTypePath(item.sChunkType) + name + item.iInstance + ".htm";
                        break;
                    }
                    if (!"".equals(name.trim()))
                    {
                        sHtmlPreInterfaces = sHtmlPreInterfaces + configModel.HtmlLoopInterfaces.replace("[%ITEMLINK%]", newValue).replace("[%NAME%]", name).replace("[%TYPE%]", str4).replace("[%PARAMETERS%]", paramsData);
                    }
                }
            }
            return (sHtmlPreInterfaces + configModel.HtmlPostInterfaces);
    }
    
    //**************************************************************************
    // Method: createSnippetFiles
    // 
    // 
    //**************************************************************************
    private void createSnippetFiles(String outputName, List<String> paramItems, boolean snippetDescr, boolean snippetExample, boolean snippetInput, boolean snippetOutput, String snippetMarker) {
        String str = outputName.substring(0, outputName.lastIndexOf("\\"));

//        String message = String.format("WARNING: %s %d", outputName, paramItems.size());
//        debugOut(message);
        if (!str.endsWith("\\")) {
            str = str + "\\";
        }
        if (snippetDescr) {
            createSnippetFile(outputName + "-descr.flsnp", snippetMarker);
        }
        if (snippetExample) {
            createSnippetFile(outputName + "-example.flsnp", snippetMarker);
        }
        if (snippetInput) {
            createSnippetFile(outputName + "-input.flsnp", snippetMarker);
        }
        if (snippetOutput) {
            createSnippetFile(outputName + "-output.flsnp", snippetMarker);
        }

        for (String item : paramItems) {
            String text = snippetMarker;

            String value = "";
                // See if the item contains the key
            //var kvpResult = new Map<String, String>();
            for (String key : snippetText.keySet()) {
                if (item.contains(key)) {
                    // Get the String value that goes with the key 

                    value = snippetText.get(key);
                    break;
                }
            }
            if (false == value.equals("")) {
                text = value;
            }

//            if(item.contains("prop"))
//            {
//                debugOut("DEBUG: prop found");
//            }
            createSnippetFile(str + item + ".flsnp", text);
        }
    }

    //**************************************************************************
    // Method: createSnippetFile
    // 
    // 
    //**************************************************************************
    private void createSnippetFile(String filename, String snippetMarker) {
        createdSnippetFiles++;
        
        try {
            File file = new File(filename);

            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                writer.write("<html xmlns:MadCap=\"http://www.madcapsoftware.com/Schemas/MadCap.xsd\" MadCap:lastBlockDepth=\"2\" MadCap:lastHeight=\"140\" MadCap:lastWidth=\"802\">");
                writer.write("    <body>");
                writer.write("    <!--Insert text, data etc. here-->");
                writer.write("    <p>&#160;" + snippetMarker + "</p>");
                writer.write("    </body>");
                writer.write("</html>");
                writer.flush();
            }
        } catch (IOException e) {
            debugOut(e.getMessage());
        }
    }
    
    private String getDocData(String sPlaceholder, String sName, String sParentClass) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    //**************************************************************************
    // Method: StoreTypeLink
    // Called when we need to store that a type is being 
    // used/referenced somewhere in the system, this routine stores the
    // 	details so that we can build the 'Used By...' output
    //**************************************************************************
    private void storeTypeLink(String sItemName, String sFilename, String sUsedByName, String sUsedByType) {
        boolean bFound = false;
        ItemUsage iuUsage = new ItemUsage();
        ItemUsage iuCheck = null;

        if (!sItemName.toUpperCase().trim().contains("<A HREF=")) //Does the item name contain a hyperlink? 
        {
            return;														//No - We can't link to it then (so get out and don't store it) 
        }
        //Extract the item name form the hyperlink
        sItemName = sItemName.substring(sItemName.indexOf("\">") + 2);
        sItemName = sItemName.substring(0, sItemName.indexOf("<")).trim();

        if ((!sItemName.trim().equals("")) //Do we have a name? 
                && (!sUsedByName.trim().equals(""))) {											//Yes - Build a Usage structure
            iuUsage.sItemName = sItemName;
            iuUsage.sUsedByName = sUsedByName;
            iuUsage.sFilename = sFilename;
            iuUsage.sUsedByType = sUsedByType;

			//We have our usage structure *but* we don't want repeated values (as it
            //would result in the 'Used By...' list containing duplicate entries). 
            //So...check to see if we already have an entry with the same data...
            for (int iCount = 0; iCount < alItemUsage.size(); iCount++) {
                iuCheck = (ItemUsage) alItemUsage.toArray()[iCount];

                if ((sFilename.equals(iuCheck.sFilename)) //If this item matches our new item... 
                        && (sUsedByName.equals(iuCheck.sUsedByName))
                        && (sUsedByType.equals(iuCheck.sUsedByType))
                        && (sItemName.equals(iuCheck.sItemName))) {
                    bFound = true;									//...flag it and get out
                    break;
                }
            }

            if (!bFound) //Did we already find a matching item?
            {
                alItemUsage.add(iuUsage);		//No - Add it to the list
            }
        }
    }

    //**************************************************************************
    // Method: buildTypeLink
    // Given a set of data about a type, this routine bundles/formats
    // the type so that, if necessary, it includes a hyperlink to the
    // documentation for the type
    //**************************************************************************
    private String buildTypeLink(String sType, String sParentClass, boolean bItalic, boolean bAsText) {
        String sResult = "";
        String sPrepend = "";
        String sAppend = "";
        String sFindType = "";
        String sFindClass = "";
        String sPath = "";
        String sLink = "";
        ItemData objItem = null;

        if (sType.contains("<")) //Is the type a list/set/map (e.g. List<Integer>)?
        {								//Yes - Get the *inner* type (e.g. Integer)
            sPrepend = sType.substring(0, sType.indexOf("<") + 1).trim();
            sAppend = "&gt;";

            sPrepend = sPrepend.replace("<", "&lt;");
            sPrepend = sPrepend.replace(">", "&gt;");

            sType = sType.substring(sType.indexOf("<") + 1).trim();

            if (sType.endsWith(">")) {
                sType = sType.substring(0, sType.length() - 1).trim();
            }
        }

        if (sType.trim().endsWith("[]")) //Is this old notation for an array?
        { 										//Yes - 'Translate' it into new notation
            sPrepend = "array&lt;";
            sAppend = "&gt;";

            sType = sType.trim();
            sType = sType.substring(0, sType.length() - 2).trim();
        }

        if (sType.contains(".")) //Is the type within another class?
        {										//Yes - Extract *only* the type 
            sFindClass = sType.substring(0, sType.lastIndexOf(".")).trim();
            sFindType = sType.substring(sType.lastIndexOf(".") + 1).trim();
        } else {										//No - Use the whole text as the type
            sFindType = sType;
            sFindClass = sParentClass;
        }

        //Try to find the type in our list of things...
        for (int iCount = 0; iCount < alItems.size(); iCount++) {
            objItem = (ItemData) alItems.toArray()[iCount];

            //If this is the correct type...
            if ((objItem.sName.toUpperCase().trim().equals(sFindType.toUpperCase().trim()))
                    && (objItem.sParentClass.toUpperCase().trim().equals(sFindClass.toUpperCase().trim()))) {
                //...store the hyperlink to it
                sPath = getChunkTypePath(objItem.sChunkType);

                sLink = sPath + sFindType + Integer.toString(objItem.iInstance) + DefFileExt;

                break;
            }
        }

        if (!sLink.trim().equals("")) //If we have a link, create the HTML...
        {
            sResult = "<a href=\"" + sLink + "\">" + configModel.Namespace + sFindClass + "." + sFindType + "</a>";
        } else {
            sResult = sType; 			//...otherwise use the *original* type data (as it'll contain any prefix text)
        }
        if (bAsText) //Are we outputting the results as text?
        { 					//Yes - Build up the type as a more 'text like' string
            if (sPrepend.trim().equals("")) //Do we have anything to add onto the start of the results (e.g. 'List<')?
            { 									//No - Just add on 'object' (e.g. 'MyType object')
                if (!sResult.trim().equals("")) //Do we have any result data?
                {
                    sResult = sResult + " object"; 		//Yes - Just add 'object' to the end
                } else {
                    sResult = ""; 						//No...err return nothing
                }
            } else { 									//Yes - Add on the leading text and a trailing '>'
                sPrepend = sPrepend.replace("&lt;", "");
                sResult = sPrepend.toLowerCase().trim() + " of " + sResult + " objects";
            }
        } else //No - Just add on any leading and trailing data that we extracted
        {
            sResult = sPrepend + sResult + sAppend;
        }

        //If we need to make the results italic...wrap them in HTML tags
        if (bItalic && (!sResult.trim().equals(""))) {
            sResult = "<i>" + sResult + "</i>";
        }

        return sResult;
    }
    
    //**************************************************************************
    // Routine: GetChunkTypePath
    // Outline: Given a chunk type, this routine works out which output path
    //			corresponds to it
    //**************************************************************************
    private String getChunkTypePath(String sChunkType) {
        String sResult = "";

        if (sChunkType.equals(ctClass)) {
            sResult = "../Types/";
        }
        if (sChunkType.equals(ctEnum)) {
            sResult = "../Enums/";
        }
        if (sChunkType.equals(ctMethod)) {
            sResult = "../Services/";
        }
        if (sChunkType.equals(ctTest)) {
            sResult = "../Tests/";
        }
        if (sChunkType.equals(ctUnknown)) {
            sResult = "../Unknown/";
        }

        return sResult;
    }
    
    private Boolean isVowel(String sData)
        {
            sData = sData.toUpperCase().trim();
            return (sData.startsWith("A") || sData.startsWith("E")) || ((sData.startsWith("I") || sData.startsWith("O")) || sData.startsWith("U"));
        }
}
