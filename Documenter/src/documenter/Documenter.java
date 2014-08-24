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

    private static final String appName = "FinancialForce.com Documentation Generator";
    private static final String appVersion = "v12.0 (16/08/2014)";
    private static final String appCopyright = "FinancialForce.com © 2014";

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

    /**
     * Routine: Main
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Documenter ceEngine = new Documenter();

//        if (args.length >= 1) //Do we have a first parameter (config file)?
//        {
//            sConfigFile = args[0];		//Yes - Store its name
//        }
        String configFilename = "C:\\Dev_GitHub\\Documenter\\src\\ConfigFilesEtc\\V12.dcf";

        //Output copyright info, version etc.
        System.out.printf("%s %s\n%s\n\n", appName, appVersion, appCopyright);

        //Tell the user that we've started...
        System.out.printf("Generation Started: %s\n\n", new Date().toString());

        //...and *start* (by loading the configuration)
        ceEngine.loadConfiguration(configFilename);

        if (ceEngine.globalAbort) //Do we have a global abort?
        {
            System.out.printf("\n***ABORT***\n\nDebug: %s\n", ceEngine.globalAbortText);
        } else {
            //No - Run the rest of the process
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

    /**
     * Given an input path, this routine looks for .CLS files, scans them
     * (adding the results to the alItems arraylist) and then generates the
     * output documentation files
     */
    public void generate() {

        // Create the output structure
        createOutputStructure(configModel.OutputFolder);

        List<FilenameVersionFf> latestVersionFileList = scanFiles();
        if (latestVersionFileList.isEmpty()) {
            globalAbort = true;
            globalAbortText = "Could not find any files to process";
            debugOut(String.format("ABORTED: %s", globalAbortText));
            return;
        }
        processSelectedFiles(latestVersionFileList);

        if ((processedChunks == 254 || outputtedChunks == 3813) == false) {
            globalAbort = true;
            globalAbortText = "Processed file count is wrong";
            debugOut(String.format("ABORTED: %s", globalAbortText));
            return;
        }
        generateFiles();

        String message = String.format("AllItems: %s Chunks: Processed %s Outputted %s OutputChunkEnum %s ExcludedFiles: %d%s", alItems.size(), processedChunks, outputtedChunks, outputChunkEnum, excludedFiles, NewLine);
        message += String.format("CreateDocsEnums: %d CreatedSnippetFiles %d OutputChunkProperty %d", createDocsEnums, createdSnippetFiles, outputChunkProperty);
        debugOut(message);
        System.out.printf(message + NewLine);
    }

    /**
     *
     * @param configurationFileName
     */
    private void loadConfiguration(String configurationFileName) {
        configModel = ConfigurationSvc.LoadConfigFile(configurationFileName);
    }

    //**************************************************************************
    // Method: scanFile
    // Scan for the file that we are interested in and select the latest versions 
    //
    //**************************************************************************
    /**
     *
     * @return
     */
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
        if (listOfFiles == null) {
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
                    filenameVersionFf.name = filenameSplitStrings[0];
                    filenameVersionFf.fullName = file.getPath();
                    filenameVersionFf.version = version;

                    int result = getIndexOf(latestVersionFileList, filenameVersionFf);
                    if (-1 == result) {
                        // Add
                        latestVersionFileList.add(filenameVersionFf);
                    } else {
                        // Check to seee if we are newer if so update
                        if (filenameVersionFf.version > latestVersionFileList.get(result).version) {
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

    /**
     *
     * @param latestVersionFileList
     */
    private void processSelectedFiles(List<FilenameVersionFf> latestVersionFileList) {
        Integer totalFiles = 0;

        List<String> excludeList = configModel.AllExcludeFiles;
        for (FilenameVersionFf fileName : latestVersionFileList) {
            String name = fileName.name;
            if (name != null) {
                name = name.toLowerCase().trim();

//                    String testCatch = "CODAAPIGeneralLedgerAccountTypes";
//                    if (name.toUpperCase().startsWith(testCatch.toUpperCase())) {
//                        DebugOut(name);
//                    }
                if (!excludeList.contains(name)) {
                    totalFiles++;
                    scanFile(fileName.fullName);
                } else {
                    excludedFiles++;
                    System.out.printf(fileName.fullName + " not scaned " + excludedFiles);
                }
            }
        }
    }

    /**
     * Scan for the file that we are interested in and select the latest
     * versions
     *
     * @param filename
     */
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

    /**
     * This routine attempts to create the output folder structure used to hold
     * the generated files
     *
     * @param outputFolder
     */
    private void createOutputStructure(String outputFolder) {

        outputFolder = outputFolder.trim();

        //Create the root output folder
        (new File(outputFolder)).mkdir();

        //Does our output folder end with a '\'?
        if (!outputFolder.endsWith("\\")) {
            //Nope - Add one on
            outputFolder += "\\";
        }

        //Create the individual item type output folders
        (new File(outputFolder + EnumsFolder)).mkdirs();
        (new File(outputFolder + InterfacesFolder)).mkdirs();
        (new File(outputFolder + ServicesFolder)).mkdirs();
        (new File(outputFolder + TestsFolder)).mkdirs();
        (new File(outputFolder + TypesFolder)).mkdirs();
        (new File(outputFolder + UnknownFolder)).mkdirs();
        (new File(outputFolder + TocFolder)).mkdirs();
        (new File(outputFolder + SnippetsFolder)).mkdirs();
        System.out.println("Done");
    }

    /**
     *
     */
    private void generateFiles() {
        if (alItems.isEmpty()) {
            debugOut("There is no class data to use in the generation process. Please check your input paramaters and scan classes again.");
        } else {
            configModel.AlHtml.add(configModel.HtmlPreEnumTable);
//                configModel.HtmlPreEnumTable = FormatPlaceholders(configModel.AlHtml[0].toString(), null);

            if ((!"".equals(configModel.Namespace.trim())) && !configModel.Namespace.endsWith(".")) {
                configModel.Namespace += ".";
            }

            generateOutputDocs(configModel.OutputFolder);
        }
    }

    /**
     * Given an output folder, this routine checks which type(s) of items we
     * want to create documents for and then creates them
     *
     * @param outputFolder
     */
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

    /**
     *
     * When called, this routine reads in the specified template file (if it
     * exists) and then attempts to use that file's data to create a file for
     * each thing of the type specified (e.g. method, type). To do this it uses
     * the placeholder in the template file to insert values from the thing into
     * HTML text...and then saves the resultant text to an output file
     *
     * @param docPath
     * @param rootPath
     * @param docType
     * @param docTemplate
     * @param tocList
     * @param createFiles
     */
    private void CreateDocs(String docPath, String rootPath, String docType, String docTemplate, List<String> tocList, Boolean createFiles) {
        String path = "";
        String sData = "";
        ArrayList<String> alDocPlaceholders = new ArrayList<String>();
        docTemplate = docTemplate.trim();
        if (!"".equals(docTemplate)) {
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
                if (itemData.outputFile.startsWith("CODA")) {
                    //System.Diagnostics.Debug.WriteLine(objItem.sOutputFile);
                    continue;
                }

                // This is for debugging
                if ((itemData.chunkType == null ? docType == null : itemData.chunkType.equals(docType)) && (docType == null ? ctEnum == null : docType.equals(ctEnum))) {
                    createDocsEnums++;
                }

                if (((itemData.chunkType == null ? docType == null : itemData.chunkType.equals(docType))
                        && (((!"".equals(itemData.parentClass.trim())) || !configModel.SkipRootClasses) || (!itemData.chunkType.equals(ctClass))))
                        && (((!itemData.chunkType.equals(ctMethod)) || (itemData.methodType != 1)) || !configModel.SkipConstructor)) {

                    //Are we creating files?
                    if (createFiles) {
                        path = docPath + itemData.outputFile;

                        //Does the output file already exist?
                        if ((new File(path)).isFile()) {
                            //Yes - Work out what to do. Are we overwriting?
                            if (!configModel.bOverwriteDocs) {
                                //No - Create a backup of the file
                                String sBackupFile = DateToShortDateString(new Date()) + " " + DateToShortTimeString(new Date());
                                sBackupFile = sBackupFile.replace("\\", "-").replace(":", "-").replace("/", "").trim();

                                sBackupFile = path + itemData.name + Integer.toString(itemData.instance) + "-" + sBackupFile + DefFileExt;

                                try {
                                    copyFile(path, sBackupFile);
                                } catch (IOException e) {
                                }
                            }

                            (new java.io.File(path)).delete();
                        }
                    }
                    String source = itemData.outputFile;
                    if (source.contains(".")) {
                        source = source.substring(0, source.indexOf('.')).trim();
                    }
                    String sOutputMethodName = GenerateSnippetFilename(itemData);

                    //if (itemData.sName.ToUpper() == "HEADERDETAILS")
                    //{
                    //    sName = itemData.sName;
                    //}
                    String sName = sData;
                    String sUsedByName = itemData.name;
                    if (!"".equals(itemData.parentClass.trim())) {
                        sUsedByName = itemData.parentClass + "." + sUsedByName;
                    }
                    sUsedByName = configModel.Namespace + sUsedByName;
                    configModel.SnippetsList.clear();

                    sName = sName.replace("[%FULLNAME%]", sUsedByName);
                    sName = sName.replace("[%NAME%]", itemData.name);
                    sName = sName.replace("[%ITEMTYPE%]", itemData.chunkType);
                    sName = sName.replace("[%FILENAME%]", itemData.sourceFile);
                    sName = sName.replace("[%INPUTPARAMS%]", createParamsData(sOutputMethodName, sUsedByName, itemData.instance, itemData.chunkType, itemData.params, itemData.parentClass, "", "", false));
                    sName = sName.replace("[%INPUTPARAMSCR%]", createParamsData(sOutputMethodName, sUsedByName, itemData.instance, itemData.chunkType, itemData.params, itemData.parentClass, NewLine, configModel.ParamIndent, false));
                    sName = sName.replace("[%INPUTPARAMSTABLE%]", createParamsData(sOutputMethodName, sUsedByName, itemData.instance, itemData.chunkType, itemData.params, itemData.parentClass, "", "", true));
                    sName = sName.replace("[%PARENT%]", itemData.parentClass).replace("[%RETURNTYPE%]", buildReturnLink(itemData.methodType, sUsedByName, itemData.instance, itemData.chunkType, itemData.type, itemData.parentClass, false));
                    sName = sName.replace("[%RETURNTYPETEXT%]", buildReturnLink(itemData.methodType, sUsedByName, itemData.instance, itemData.chunkType, itemData.type, itemData.parentClass, true));
                    sName = sName.replace("[%VALUES%]", itemData.values).replace("[%VALUESTABLE%]", CreateValuesTable(sOutputMethodName, itemData.values));
                    sName = sName.replace("[%USAGELIST%]", createUsageList(itemData.name, itemData.parentClass));
                    sName = sName.replace("[%INTERFACELIST%]", createInterfaceList(itemData.name, itemData.instance, itemData.chunkType, itemData.chunkData, itemData.parentClass));
                    sName = sName.replace("[%PROPERTYLIST%]", createPropertyList(sOutputMethodName, itemData.name, itemData.parentClass, itemData.extendsClass, itemData.chunkType, itemData.instance));
                    sName = sName.replace("[%VISIBILITY%]", itemData.visibility);
                    sName = sName.replace("[%RAWDATA%]", itemData.chunkData);
                    sName = sName.replace("[%PRODUCTNAMESPACE%]", configModel.Namespace);
                    sName = sName.replace("[%OUTPUTNAME%]", source);
                    sName = sName.replace("[%OUTPUTFILENAME%]", itemData.outputFile);
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
                        String newValue = getDocData(sPlaceholder, itemData.name, itemData.parentClass);
                        sName = sName.replace(sPlaceholder, newValue);
                    }

//                        StatusMsg = "Generating:";
                    if (createFiles) {
                        //Yes - Create the file (and create an entry in the TOC if we have to)
                        //We have the data for this file...so save it

                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                            writer.write(sName);
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                        }

                        if (configModel.Toc != 0) {
                            tocList.add(itemData.chunkType + "[" + sUsedByName + "]" + path);
                        }
                        if (configModel.IncludeSnippets) {
                            createSnippetFiles(String.format("%s%s\\%s", rootPath, SnippetsFolder, sOutputMethodName), configModel.SnippetsList, bSnippetDescr, bSnippetExample, bSnippetInput, bSnippetOutput, configModel.SnippetMarker);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param latestVersionFileList
     */
    private void dumpFiles(List<FilenameVersionFf> latestVersionFileList) {
        String filesProcessedFileName = configModel.OutputFolder + "\\FilesProcessed.txt";

        //Does the output file already exist?
        if ((new File(filesProcessedFileName)).isFile()) {
            (new File(filesProcessedFileName)).delete();
        }

        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filesProcessedFileName))) {
                for (FilenameVersionFf file : latestVersionFileList) {
                    writer.write(file.fullName + "\n");
                }
            }
        } catch (IOException e) {
        }
    }

    /**
     *
     */
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

    /**
     *
     * @param message
     */
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

                String debugMessage = String.format("%s ********* %s%s%s", dateTime, NewLine, message, NewLine);
                // Writes the content to the file
                writer.write(debugMessage);
                writer.flush();
            }
        } catch (IOException e) {
        }
    }

    /**
     *
     * @param latestVersionFileList
     * @param filenameVersionFf
     * @return
     */
    private int getIndexOf(List<FilenameVersionFf> latestVersionFileList, FilenameVersionFf filenameVersionFf) {
        if (latestVersionFileList == null) {
            return -1;
        }

        int index = -1;
        for (FilenameVersionFf file : latestVersionFileList) {
            if (file.name.equals(filenameVersionFf.name)) {
                index = latestVersionFileList.indexOf(file);
            }
        }
        return index;
    }

    /**
     * Given a set of data, this routine works out the next 'chunk' is i.e.
     * section, structure etc, and returns it
     *
     * @param data
     * @param chunkType
     * @return
     */
    private String getNextChunk(RefObject<String> data, RefObject<String> chunkType) {
        String result = "";
        boolean bHandled = false;

        // This is for debugging
        String savedData = data.argvalue;

        //Assume that we have no idea what this chunk is (as we don't at this point)
        chunkType.argvalue = ctUnknown;

        //Does the data start with a '//' (i.e. is this a comment)?
        if (data.argvalue.startsWith("//")) {
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
        if (data.argvalue.startsWith("/*")) {
            //Yes - Extract the comment, set the return type...and get out
            result = extractText(data, data.argvalue.indexOf("*/") + 1);
            chunkType.argvalue = ctComment;
            return result;
        }

        String start;
        //Does this data have a space in it?
        if (data.argvalue.indexOf(" ") >= 0) {
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
            if (iSemiColon != -1) {
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
                if ((iBrace < iEquals) && (iBrace != -1)) {
                    iChunkEnd = 2;
                    //Yes - Class or routine

                    //Regular bracket before brace bracket?
                    if ((iBracket < iBrace) && (iBracket != -1)) {
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
                if ((iBrace < iBracket) && (iBrace != -1)) {
                    //Yes - Enumeration
                    iChunkEnd = 1;
                    chunkType.argvalue = ctEnum;
                } else {
                    //Regular bracket before brace bracket?
                    if ((iBracket < iBrace) && (iBracket != -1)) {
                        iChunkEnd = 2;
                        chunkType.argvalue = ctMethod;
                    } else {
                        //Brace and *no* regular bracket?
                        if ((iBrace != -1) && (iBracket == -1)) {
                            iChunkEnd = 2;
                            //chunkType.argvalue = ctProperty;
                            chunkType.argvalue = data.argvalue.startsWith("enum ") ? "Enumerations" : "Property";
                        } else {
                            //No brace, no bracket and no equals?
                            if ((iBrace == -1) && (iBracket == -1)) {
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
                    && (data.argvalue.toLowerCase().indexOf("class") >= 0)) {
                //Yes - Read to the end of it
                iChunkEnd = 2;
                chunkType.argvalue = ctClass + "3";
            }

            if (check.equals("@REMOTEACTION")) {
                chunkType.argvalue = ctRemoteAction;
                result = "";
            }

            //If this method is actually a test...
            if ((chunkType.argvalue.equals(ctMethod)) && (check.equals("@ISTEST"))) {
                //...mark it as such
                chunkType.argvalue = ctTest;
            }

            if ((chunkType.argvalue.equals(ctMethod)) && (check.equals("WEBSERVICE"))) {
                chunkType.argvalue = ctWebServices;
            }

            if (data.argvalue.toUpperCase().trim().startsWith("INTERFACE")) {
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
                        if (("".equals(data.argvalue)) && (iIndent != 0)) {
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

    /**
     * This routine works out the next chunk and decides what type it is
     *
     * @param data
     * @param parentClass
     * @param filename
     */
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

    /**
     *
     * @param chunkType
     * @param chunk
     * @param parentClass
     * @param filename
     */
    private void outputChunk(String chunkType, String chunk, String parentClass, String filename) {
        outputtedChunks++;

        if (!"".equals(chunk.trim())) {
            String str;

            if (((chunkType.equals("Comment")) || (chunkType.equals("Unknown"))) || (chunkType.equals("Tests"))) {
                str = "N/A";
            } else {
                if (chunk.toUpperCase().startsWith("STATIC ")) {
                    chunk = chunk.substring(7).trim();
                }

                if (chunk.trim().contains(" ")) {
                    str = chunk.substring(0, chunk.indexOf(" ")).toUpperCase();
                } else {
                    str = chunk.toUpperCase().trim();
                }
            }

            if ((((str.equals("PUBLIC")) && configModel.ShowPublic) || ((str.equals("PRIVATE")) && configModel.ShowPrivate))
                    || ((((str.equals("GLOBAL")) && configModel.ShowGlobal) || ((str.equals("WEBSERVICE")) && configModel.ShowWebService)) || (str.equals("N/A")))) {
                ItemData newItem = new ItemData();
                newItem.name = "";
                newItem.parentClass = parentClass;
                newItem.chunkType = chunkType;
                newItem.chunkData = chunk;
                newItem.visibility = str;
                newItem.sourceFile = filename;
                newItem.instance = 1;

                switch (chunkType) {
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
                        newItem.chunkType = "Methods";
                        newItem.remoteAction = true;
                        newItem = decodeMethod(newItem);
                        break;

                    default:
                        String message = String.format("ERROR: This item was not outputted *********** %s", chunkType);
                        debugOut(message);
                        break;
                }

                Boolean debugPrint = false;
                if (newItem.name.startsWith("enumCreditNoteStatus")) {
                    //debugOut("DEBUG: " + newItem.sName);
                    debugPrint = true;
                }

                //Did we identify a name for this chunk?
                if (!"".equals(newItem.name.trim())) {
                    for (ItemData data2 : alItems) {
//                            if (data2.sName.startsWith("enumCreditNoteStatus"))
//                            {
//                                debugOut("DEBUG: " + data2.sName);
//                            }
//
//                            if ("Enumerations".equals(newItem.chunkType) && (data2.chunkType == null ? ctEnum == null : data2.chunkType.equals(ctEnum)))
//                            {
//                                if(data2.sName.toUpperCase().trim() == null ? newItem.sName.toUpperCase().trim() == null : data2.sName.toUpperCase().trim().equals(newItem.sName.toUpperCase().trim()))
//                                {
//                                    if (newItem.sName.startsWith("enumCreditNoteStatus"))
//                                    {
//                                        debugOut("DEBUG: " + data2.sName);
//                                    }                                    
//                                }
//                            }
                        if (((data2.chunkType == null ? newItem.chunkType == null : data2.chunkType.equals(newItem.chunkType)) && (data2.name.toUpperCase().trim() == null ? newItem.name.toUpperCase().trim() == null : data2.name.toUpperCase().trim().equals(newItem.name.toUpperCase().trim()))) && (data2.instance >= newItem.instance)) {
                            newItem.instance = data2.instance + 1;
                        }
                    }
                    newItem.outputFile = newItem.name + newItem.instance + ".htm";
                    alItems.add(newItem);
//                        if(debugPrint)
//                        {
//                            debugOut("DEBUG: allItems.add   " + newItem.sOutputFile + "    " + newItem.chunkType);
//                        }
                }
            }
        }
    }

    /**
     * Given a set of object data containing a class, this routine extracts the
     * class name and whether it extends another class
     *
     * @param newItem
     * @return
     */
    private ItemData decodeClass(ItemData newItem) {
        newItem.name = extractItemName(newItem, "CLASS");

        //Work out if this class extends another class...and store it if it does
        if (newItem.chunkData.toLowerCase().contains(" extends ")) //Does the chunk data contain 'extends'?
        {
            //Yes - Work out *what* it extends
            newItem.extendsClass = newItem.chunkData.substring(newItem.chunkData.toLowerCase().indexOf(" extends ") + 8).trim();

            if (newItem.extendsClass.endsWith("{")) {
                newItem.extendsClass = newItem.extendsClass.substring(0, newItem.extendsClass.length() - 1).trim();
            }
        }

        return newItem;
    }

    /**
     * Given a set of object data containing an enumeration, this routine
     * extracts the enumeration name and its values
     *
     * @param newItem
     * @return
     */
    private ItemData decodeEnum(ItemData newItem) {
        newItem.name = extractItemName(newItem, "ENUM");

        String str = preProcessComments(newItem.chunkData, true);

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
        newItem.values = str;
        return newItem;
    }

    /**
     * Given a set of object data containing a variable definition, this routine
     * extract the variable's name and type
     *
     * @param newItem
     * @return
     */
    private ItemData decodeVar(ItemData newItem) {
        String sData = newItem.chunkData;

        if (sData.contains("=")) //Does the data a have a '=' in it?
        {
            //Yes - The variable has a default value...so get everything up to the '='
            sData = sData.substring(0, sData.indexOf("=") + 1).trim();
            sData = sData.replace("=", "").trim();
        } else {
            //No - The variable is *just* the variable...so get everything up to the semi-colon
            sData = sData.replace(";", "").trim();
        }

        //Does the remaining data have a space in it?
        if (sData.contains(" ")) {
            //Yes - It's well formed then (type *and* name)...so use it
            newItem.name = sData.substring(sData.lastIndexOf(" ")).trim();
            sData = sData.substring(0, sData.lastIndexOf(" ")).trim();

            newItem.type = extractReturnType(sData);
        }

        return newItem;
    }

    /**
     * Given a set of object data containing a method definition, this routine
     * extracts the method name, return type, parameter list and whether it's a
     * constructor or not
     *
     * @param newItem
     * @return
     */
    private ItemData decodeMethod(ItemData newItem) {
        String sChunkData = newItem.chunkData;
        sChunkData = sChunkData.substring(0, sChunkData.indexOf("(")).trim();
        newItem.name = sChunkData.substring(sChunkData.lastIndexOf(" ")).trim();
        sChunkData = sChunkData.substring(0, sChunkData.lastIndexOf(" ")).trim();
        newItem.type = extractReturnType(sChunkData);
        if ("[CONSTRUCTOR]".equals(newItem.type)) {
            newItem.type = "";
            newItem.methodType = 1;
        }
        if ("WEBSERVICE".equals(newItem.visibility.toUpperCase().trim())) {
            newItem.methodType = 2;
        }
        sChunkData = newItem.chunkData;
        sChunkData = sChunkData.substring(sChunkData.indexOf("(") + 1).trim();
        sChunkData = sChunkData.substring(0, sChunkData.indexOf(")")).trim();
        newItem.params = sChunkData;
        return newItem;
    }

    /**
     * Given a set of object data containing an unknown, this routine does
     * nothing.
     *
     * @param newItem
     * @return
     */
    private ItemData decodeUnknown(ItemData newItem) {
        return newItem;
    }

    /**
     * Given a set of object data containing a set of comment data, this routine
     * re-formats the comment into a single line (if it spanned multiple lines)
     * and extracts any 'DOC' data
     *
     * @param newItem
     * @return
     */
    private ItemData decodeComment(ItemData newItem) {
        int length = 0;

        //Replace any tabs with single spaces
        String str = newItem.chunkData.replace("\t", " ").trim();

        //If this is a single line comment...remove the leading '//'                
        if (str.startsWith("//")) {
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
            newItem.name = str.substring(0, str.indexOf(" ")).trim();
            newItem.params = str.substring(str.indexOf(" ")).trim();
            return newItem;
        } else {
            newItem.name = ""; //This *isn't* a set of 'doc' data...so forget about it
        }
        return newItem;
    }

    /**
     * Given a set of object data containing a property definition, this routine
     * extracts the property name and type
     *
     * @param newItem
     * @return
     */
    private ItemData decodeProperty(ItemData newItem) {
        String sData = newItem.chunkData;

        //Extract the name
        sData = sData.substring(0, sData.indexOf("{")).trim();
        newItem.name = sData.substring(sData.lastIndexOf(" ")).trim();

        //Extract the return type
        sData = sData.substring(0, sData.lastIndexOf(" ")).trim();
        newItem.type = extractReturnType(sData);

        return newItem;
    }

    /**
     * Given a set of object data containing a test method, this routine
     * extracts everything that it can about the test (by treating it as a
     * regular method)
     *
     * @param newItem
     * @return
     */
    private ItemData decodeTest(ItemData newItem) {
        newItem = decodeMethod(newItem);

        return newItem;
    }

    /**
     * Given a set of object data containing an interface definition, this
     * routine extracts the interface name and type
     *
     * @param newItem
     * @return
     */
    private ItemData decodeInterface(ItemData newItem) {
        String sChunkData = newItem.chunkData;
        sChunkData = sChunkData.substring(sChunkData.toUpperCase().indexOf("INTERFACE ")).trim();
        sChunkData = sChunkData.substring(sChunkData.indexOf(" ")).trim();
        if (sChunkData.contains("{")) {
            sChunkData = sChunkData.substring(0, sChunkData.indexOf("{")).trim();
        }
        newItem.name = sChunkData;
        return newItem;
    }

    /**
     *
     * @param data
     * @param removeComments
     * @return
     */
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

    /**
     * Given a set of data and a piece of text to search for, this routine
     * attempts to work out the name of the data (e.g. method name, parameter
     * name)
     *
     * @param newItem
     * @param findText
     * @return
     */
    private String extractItemName(ItemData newItem, String findText) {
        String sData = newItem.chunkData;

        //Does the data contain the string that we're looking for?
        if (!sData.toUpperCase().contains(findText.toUpperCase())) {
            return "";
        }
        //Extract the data *after* the text that we're looking for
        sData = sData.substring(sData.toUpperCase().indexOf(findText.toUpperCase().trim() + " ") + (findText.trim().length() + 1)).trim();

        if (sData.contains(" ")) {
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

    /**
     * Given a text string, this routine returns the section of text up to the
     * supplied position. As well as this it also removes that section of text
     * from the original text
     *
     * @param data
     * @param toPos
     * @return
     */
    private String extractText(RefObject<String> data, int toPos) {
        String sResult = "";

        //Is the position past the end of the source text?
        if (toPos >= data.argvalue.length()) {
            sResult = data.argvalue;
            data.argvalue = "";
        } else {
            //No - Return only the section of text we were asked for
            if (toPos >= 0) {
                //Extract the text...
                sResult = data.argvalue.substring(0, toPos + 1).trim();

                //...and remove it form the source text
                data.argvalue = data.argvalue.substring(toPos + 1).trim();
            }
        }

        return sResult;
    }

    /**
     * Given a text string (containing something like a method or list
     * definition) this routine works out the return type
     *
     * @param data
     * @return
     */
    private String extractReturnType(String data) {
        if (data.endsWith(">")) {
            String str = data.substring(data.indexOf("<")).trim();
            data = data.substring(0, data.indexOf("<")).trim();

            if (data.contains(" ")) {
                str = data.substring(data.lastIndexOf(" ")).trim() + str;
            } else {
                str = data.trim() + str;
            }
            return str.replace(" ", "").replace(",", ", ");
        }
        if (data.contains(" ")) {
            return data.substring(data.lastIndexOf(" ")).trim();
        }
        return "[CONSTRUCTOR]";
    }

    /**
     * Given a text string and a pattern to find this routine returns the number
     * of instances of the pattern in the text
     *
     * @param text
     * @param pattern
     * @return
     */
    private int textCount(String text, String pattern) {
        int iCount = 0;
        int iIndex = 0;

        //Loop until we can't find any more instances of the pattern...
        while ((iIndex = text.indexOf(pattern, iIndex)) != -1) {
            iIndex += pattern.length();
            iCount += 1;
        }
        //All done, return how many instances we found
        return iCount;
    }

    /**
     * This routine takes the supplied text and converts any placeholder's
     * ('[%<Name>%]' to uppercase (making them easier to match in the future)
     *
     * @param data
     * @param alDocPlaceholders
     * @return
     */
    private String FormatPlaceholders(String data, ArrayList<String> alDocPlaceholders) {
        String sPlaceholder = "";
        StringBuilder sbResult = new StringBuilder();

        while (data.contains("[%")) //Loop until we run out of placeholders...
        {
            //Add everything *up to* the placeholder to the output data
            sbResult.append(data.substring(0, data.indexOf("[%")));

            //Remove everything *up to* the placeholder form the source data
            data = data.substring(data.indexOf("[%"));

            if (data.contains("%]")) //If the source data contains the *end* of a placeholder...
            { //...extract the placeholder
                //Extract the placeholder and convert it to uppercase
                sPlaceholder = data.substring(0, data.indexOf("%]") + 2).toUpperCase().trim();
                data = data.substring(data.indexOf("%]") + 2);

                sbResult.append(sPlaceholder); //Add the placeholder to the output data

                if (alDocPlaceholders != null) //Are we building a list of 'DOC' placeholders?
                {
                    //Yes - Add this to the list (Assuming that it's a 'DOC' placeholder of course)
                    if (sPlaceholder.startsWith("[%DOC:") && (alDocPlaceholders.indexOf(sPlaceholder) < 0)) {
                        alDocPlaceholders.add(sPlaceholder);
                    }
                }
            }
        }

        sbResult.append(data); //Add any remaining data to the output

        return sbResult.toString();
    }

    /**
     *
     * @param tocList
     * @param outputFolder
     */
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

    /**
     *
     * @param tocList
     * @param outputFolder
     */
    private void GenerateFlareToc(ArrayList<String> tocList, String outputFolder) {
        String upperCaseHeading = "";
        String tempSource = "";

        java.util.Collections.sort(tocList);

        if (!outputFolder.endsWith("\\")) {
            outputFolder += "\\";
        }

        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder + "PrimaryTOC.fltoc"))) {

                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + NewLine);
                writer.write("<CatapultToc" + NewLine);
                writer.write("  Version=\"1\">" + NewLine);
                if (!"".equals(configModel.TocPreNodes.trim())) {
//                writer.write(TocLinks.GenerateFlareXml(configModel.TocPreNodes).trim());
                }
                writer.write("  <TocEntry" + NewLine);
                writer.write("    Title=\"API Reference\"" + NewLine);

                writer.write("    Link=\"/Content/Reference/Reference.htm\">" + NewLine);

                for (String tocItem : tocList) {
                    String heading = extractSubString(0, tocItem, "[", 0).trim();
                    String fullFilename = extractSubString(tocItem, "]", 1).trim();
                    String displayText = extractSubString(tocItem, "[", 1).trim();
                    displayText = extractSubString(0, displayText, "]", 0).trim();

                    String source;
                    if (!heading.toUpperCase().trim().equals(upperCaseHeading)) {
                        if (!"".equals(upperCaseHeading.trim())) {
                            writer.write("      </TocEntry>" + NewLine);
                            writer.write("    </TocEntry>" + NewLine);
                        }
                        source = displayText;
                        if (!"".equals(configModel.Namespace.trim())) {
                            source = source.substring(configModel.Namespace.length()).trim();
                        }
                        if (source.contains("\\.")) {
                            source = source.substring(0, source.indexOf(".")).trim();
                        }
                        tempSource = source;
                        writer.write("    <TocEntry" + NewLine);
                        writer.write("      Title=\"" + heading + "\">" + NewLine);
                        writer.write("      <TocEntry" + NewLine);
                        writer.write("        Title=\"" + source + "\">" + NewLine);
                        upperCaseHeading = heading.toUpperCase().trim();
                    } else {
                        source = displayText;
                        if (configModel.Namespace.trim() != "") {
                            source = source.substring(configModel.Namespace.length()).trim();
                        }
                        if (source.contains("\\.")) {
                            source = source.substring(0, source.indexOf(".")).trim();
                        }
                        if (!tempSource.equals(source)) {
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

                    if (!configModel.IncludeTocFullNames && displayText.contains("\\.")) {
                        displayText = extractSubString(displayText, ".", 1).trim();
                    }
                    writer.write("        <TocEntry" + NewLine);
                    writer.write("          Title=\"" + displayText + "\"" + NewLine);
                    writer.write("          Link=\"/Content/Reference/" + subFolderAndFilename + "\" />" + NewLine);
                    //writer.write(string.Format("          Link=\"/Content/Reference/{0}/{1} \" />", typeName, fileName));
                }
                if (!"".equals(upperCaseHeading.trim())) {
                    writer.write("      </TocEntry>" + NewLine);
                    writer.write("    </TocEntry>" + NewLine);
                }
                writer.write("  </TocEntry>" + NewLine);
                if (configModel.TocPostNodes.trim() != "") {
//                writer.write(TocLinks.GenerateFlareXml(configModel.TocPostNodes).trim());
                }
                writer.write("</CatapultToc>" + NewLine);
                writer.flush();
                writer.close();

            }
        } catch (IOException e) {
        }
    }

    /**
     * Given a source file and a destination file, this routine, copies from the
     * source file to the destination file
     *
     * @param sourceFilename
     * @param destFilename
     * @throws IOException
     */
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

    /**
     * Given a set of delimited text, this routine converts that text into a
     * list and then uses it to construct a HTML table (with one item in the
     * list having one row in the output table). The HTML used to construct the
     * table is that specified in the configuration file
     *
     * @param outputMethodName
     * @param data
     * @return
     */
    private String CreateValuesTable(String outputMethodName, String data) {
        String[] strArray = data.split("\\,");
        String sHtmlPreEnumTable = configModel.HtmlPreEnumTable;
        for (String item : strArray) {
            String newValue;
            if ("".equals(item.trim())) {
                newValue = "";
            } else {
                configModel.SnippetsList.add(outputMethodName + "-value-" + item.trim());
                newValue = "<MadCap:snippetBlock src=\"../../Resources/Snippets/" + outputMethodName + "-value-" + item.trim() + ".flsnp\" />";
            }
            sHtmlPreEnumTable = sHtmlPreEnumTable + configModel.HtmlLoopEnumTable.replace("[%ENUMVALUE%]", item).replace("[%SNIPPETLINK%]", newValue);
        }
        return (sHtmlPreEnumTable + configModel.HtmlPostEnumTable);
    }

    /**
     *
     * @param itemData
     * @return
     */
    private String GenerateSnippetFilename(ItemData itemData) {
        String str = itemData.parentClass + "-" + itemData.name;
        if ((("Methods".equals(itemData.chunkType)) || (ctTest.equals(itemData.chunkType))) || (ctWebServices.equals(itemData.chunkType))) {
            if ("".equals(itemData.params.trim())) {
                return (str + "0");
            }
            String[] strArray = itemData.params.toUpperCase().split("\\,");
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

    /**
     * By using this method it aids porting between languages.
     *
     * @param data
     * @param toFind
     * @param offset
     * @return
     */
    private static String extractSubString(String data, String toFind, int offset) {
        return data.substring(data.indexOf(toFind) + offset).trim();
    }

    /**
     * By using this method it aids porting between languages.
     *
     * @param startIndex
     * @param data
     * @param toFind
     * @param offSet
     * @return
     */
    private static String extractSubString(int startIndex, String data, String toFind, int offSet) {
        return data.substring(startIndex, data.indexOf(toFind) + offSet).trim();
    }

    /**
     * Given a date, this routine formats it as HH:mm:ss format
     *
     * @param date
     * @return
     */
    private String DateToShortTimeString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * Given a date, this routine formats it in yyyy-MM-dd format
     *
     * @param date
     * @return
     */
    private String DateToShortDateString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    /**
     * Given a date, this routine formats it in dd/MM/yyyy HH:mm:ss format
     *
     * @param date
     * @return
     */
    private String DateToShortDateTimeString(java.util.Date date) {
        Format formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * Class used to simulate the ability to pass arguments by reference in
     * Java.
     *
     * @param <T>
     */
    private final class RefObject<T> {

        public T argvalue;

        public RefObject(T refarg) {
            argvalue = refarg;
        }
    }

    /**
     *
     */
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

    /**
     * Given a set of data, this routine builds an output HTML structure
     * detailing the parameters, types, hyperlinks (where necessary) etc.
     *
     * @param outputMethodName
     * @param usedByName
     * @param usedByInstance
     * @param usedByChunkType
     * @param data
     * @param parentClass
     * @param lineEnd
     * @param lineStart
     * @param table
     * @return
     */
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

//        if(outputMethodName.contains("GetInvoice"))
//            debugOut("DEBUG: " + outputMethodName);
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
            if (data.charAt(iCount) != ',') {
                sParam += data.charAt(iCount);
            }
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

    /**
     * Given a name, chunk type and instance, this routine builds a hyperlink to
     * the corresponding help file
     *
     * @param sName
     * @param sChunkType
     * @param iInstance
     * @return
     */
    private String buildUsedByLink(String sName, String sChunkType, int iInstance) {
        String sLink = sName.trim();

        if (sLink.contains(".")) //If the thing is within something else...
        {
            sLink = sLink.substring(sLink.lastIndexOf(".") + 1).trim();	//...get *only* the thing  
        }
        sLink = getChunkTypePath(sChunkType) + sLink + (new Integer(iInstance)).toString() + DefFileExt;

        return sLink;
    }

    /**
     *
     * @param methodType
     * @param usedByName
     * @param usedByInstance
     * @param usedByChunkType
     * @param returnType
     * @param parentClass
     * @param asText
     * @return
     */
    private String buildReturnLink(Integer methodType, String usedByName, int usedByInstance, String usedByChunkType, String returnType, String parentClass, boolean asText) {
        String data;

        String methodTypeTxt = "service";
        if (methodType == 1) {
            methodTypeTxt = "constructor";
        }
        if (methodType == 2) {
            methodTypeTxt = "web service";
        }
        if (asText) {
//                if(usedByName.contains("CODAAPISalesInvoice_10_0.PostIncomeSchedules"))
//                    debugOut("QWERTY: ");

            data = buildTypeLink(returnType, parentClass, false, true);
            if (("VOID".equals(data.toUpperCase().trim())) || ("".equals(data.trim()))) {
                return String.format("This %s does not return a value.", methodTypeTxt);
            }
            if (isVowel(data)) {
                return String.format("This %s returns an %s.", methodTypeTxt, data);
            }
            return String.format("This %s returns a %s.", methodTypeTxt, data);
        }

        data = buildTypeLink(returnType, parentClass, true, false);
        storeTypeLink(data, buildUsedByLink(usedByName, usedByChunkType, usedByInstance), usedByName, usedByChunkType);
        return data;
    }

    /**
     * When called, this routine takes the supplied name and builds a 'Used
     * By...' HTML structure
     *
     * @param className
     * @param parentClass
     * @return
     */
    private String createUsageList(String className, String parentClass) {
        String sHtmlPreMethodList = "";

        if (alItemUsage.size() <= 0) {
            return sHtmlPreMethodList;
        }

        //Build a list containing all of the things that refer to the item we're looking for
        List<String> usageList = new ArrayList<>();

        for (ItemUsage usage : alItemUsage) {
            if ((((usage.itemName.equals((configModel.Namespace + parentClass + "." + className)))
                    && (!"".equals(usage.usedByName.trim())))
                    && ((!"".equals(usage.itemName.trim()))
                    && (!"".equals(usage.usedByType.trim()))))
                    && (usageList.indexOf(usage.usedByName + "|" + usage.filename + "|" + usage.usedByType) == -1)) {

                //We have a set of valid data so...add it to our list of items
                usageList.add(usage.usedByName + "|" + usage.filename + "|" + usage.usedByType);
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

    /**
     * Given a set of class data, this routine creates a HTML table containing
     * all of the properties for that class (formatted as per the config file)
     *
     * @param outputMethodName
     * @param className
     * @param parentClass
     * @param extendsClass
     * @param itemChunkType
     * @param instance
     * @return
     */
    private String createPropertyList(String outputMethodName, String className, String parentClass, String extendsClass, String itemChunkType, int instance) {
        String sResult = "";
        ArrayList<String> alProps = new ArrayList<String>();

        //Does this class extend another class?
        if (!"".equals(extendsClass.trim())) {
            String sItemName = buildTypeLink(extendsClass, parentClass, false, false);
            sResult = "This class/type extends " + sItemName + ".<BR/>";
            storeTypeLink(sItemName, buildUsedByLink(className, itemChunkType, instance), configModel.Namespace + parentClass + "." + className, itemChunkType);
        }

        // Build the list of properties for this class
        for (ItemData itemData : alItems) {
            // If this thing is a property (or a variable) *and* it's parent is the class
            // we're interested in...add it to our list of properties
            if ((("Property".equals(itemData.chunkType)) || ("Variables".equals(itemData.chunkType))) && (itemData.parentClass.equals(parentClass + "." + className))) {
                alProps.add(itemData.name + "[" + itemData.type + "]");
            }
        }

        //Did we find any properties for this class?
        if (alProps.size() <= 0) {
            return sResult;
        }

        java.util.Collections.sort(alProps);

        sResult = sResult + configModel.HtmlPrePropList;

        //Iterate through the list of properties
        for (String item : alProps) {
            //Get the details of the property
            String sPropertyName = item.trim();
            String sPropertyType = extractSubString(sPropertyName, "[", 1).trim();
            if (sPropertyType.endsWith("]")) {
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

    /**
     *
     * @param className
     * @param instance
     * @param chunkType
     * @param interfaceData
     * @param parentClass
     * @return
     */
    private String createInterfaceList(String className, int instance, String chunkType, String interfaceData, String parentClass) {
        if (!ctInterfaces.equals(chunkType)) {
            return "";
        }

        if (interfaceData.contains("{")) {
            interfaceData = extractSubString(interfaceData, "{", 1);
        }
        if (interfaceData.contains("}")) {
            interfaceData = extractSubString(0, interfaceData, "}", 0);
        }

        String sHtmlPreInterfaces = "";

        String[] strArray = interfaceData.split(";");
        if (strArray.length <= 0) {
            return sHtmlPreInterfaces;
        }

        sHtmlPreInterfaces = configModel.HtmlPreInterfaces;
        for (String preItem : strArray) {
            if ("".equals(preItem)) {
                continue;
            }

            ItemData newItem = new ItemData();
            newItem.chunkData = preItem.trim();

            if (newItem.chunkData.contains("(")) {
                newItem = decodeMethod(newItem);
                String name = newItem.name;
                String paramsData = newItem.params;
                paramsData = createParamsData("", className, instance, chunkType, paramsData, parentClass, "", "", false);

                String str4;
                if ("".equals(newItem.type.trim())) {
                    str4 = "void";
                } else {
                    str4 = buildTypeLink(newItem.type, className, false, false);
                }

                String newValue = "";
                for (ItemData item : alItems) {
                    if (((!item.name.toUpperCase().trim().equals(name.toUpperCase().trim()))
                            || (!item.parentClass.toUpperCase().trim().equals(parentClass.toUpperCase().trim())))
                            || (!ctMethod.equals(item.chunkType))) {
                        continue;
                    }
                    newValue = getChunkTypePath(item.chunkType) + name + item.instance + ".htm";
                    break;
                }
                if (!"".equals(name.trim())) {
                    sHtmlPreInterfaces = sHtmlPreInterfaces + configModel.HtmlLoopInterfaces.replace("[%ITEMLINK%]", newValue).replace("[%NAME%]", name).replace("[%TYPE%]", str4).replace("[%PARAMETERS%]", paramsData);
                }
            }
        }
        return (sHtmlPreInterfaces + configModel.HtmlPostInterfaces);
    }

    /**
     *
     * @param outputName
     * @param paramItems
     * @param snippetDescr
     * @param snippetExample
     * @param snippetInput
     * @param snippetOutput
     * @param snippetMarker
     */
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

    /**
     *
     * @param filename
     * @param snippetMarker
     */
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

    /**
     * Called when we need to store that a type is being used/referenced
     * somewhere in the system, this routine stores the details so that we can
     * build the 'Used By...' output
     *
     * @param itemName
     * @param filename
     * @param usedByName
     * @param usedByType
     */
    private void storeTypeLink(String itemName, String filename, String usedByName, String usedByType) {
        boolean bFound = false;
        ItemUsage itemUsage = new ItemUsage();
        ItemUsage iuCheck = null;

        if (!itemName.toUpperCase().trim().contains("<A HREF=")) //Does the item name contain a hyperlink? 
        {
            //No - We can't link to it then (so get out and don't store it) 
            return;
        }
        //Extract the item name form the hyperlink
        itemName = itemName.substring(itemName.indexOf("\">") + 2);
        itemName = itemName.substring(0, itemName.indexOf("<")).trim();

        //Do we have a name?
        if ((!itemName.trim().equals(""))
                && (!usedByName.trim().equals(""))) {
            //Yes - Build a Usage structure
            itemUsage.itemName = itemName;
            itemUsage.usedByName = usedByName;
            itemUsage.filename = filename;
            itemUsage.usedByType = usedByType;

            //We have our usage structure *but* we don't want repeated values (as it
            //would result in the 'Used By...' list containing duplicate entries). 
            //So...check to see if we already have an entry with the same data...
            for (int iCount = 0; iCount < alItemUsage.size(); iCount++) {
                iuCheck = (ItemUsage) alItemUsage.toArray()[iCount];

                if ((filename.equals(iuCheck.filename)) //If this item matches our new item... 
                        && (usedByName.equals(iuCheck.usedByName))
                        && (usedByType.equals(iuCheck.usedByType))
                        && (itemName.equals(iuCheck.itemName))) {
                    bFound = true;
                    break;
                }
            }

            if (!bFound) //Did we already find a matching item?
            {
                alItemUsage.add(itemUsage);		//No - Add it to the list
            }
        }
    }

    /**
     * Given a set of data about a type, this routine bundles/formats the type
     * so that, if necessary, it includes a hyperlink to the documentation for
     * the type
     *
     * @param type
     * @param parentClass
     * @param asItalic
     * @param asText
     * @return
     */
    private String buildTypeLink(String type, String parentClass, boolean asItalic, boolean asText) {
        String sResult = "";
        String sPrepend = "";
        String sAppend = "";
        String sFindName = "";
        String sFindClass = "";
        String sPath = "";
        String sLink = "";
        ItemData objItem = null;

        if (type.contains("<")) //Is the type a list/set/map (e.g. List<Integer>)?
        {
            //Yes - Get the *inner* type (e.g. Integer)
            sPrepend = type.substring(0, type.indexOf("<") + 1).trim();
            sAppend = "&gt;";

            sPrepend = sPrepend.replace("<", "&lt;");
            sPrepend = sPrepend.replace(">", "&gt;");

            type = type.substring(type.indexOf("<") + 1).trim();

            if (type.endsWith(">")) {
                type = type.substring(0, type.length() - 1).trim();
            }
        }

        if (type.trim().endsWith("[]")) //Is this old notation for an array?
        {
            //Yes - 'Translate' it into new notation
            sPrepend = "array&lt;";
            sAppend = "&gt;";

            type = type.trim();
            type = type.substring(0, type.length() - 2).trim();
        }

        if (type.contains(".")) //Is the type within another class?
        {
            //Yes - Extract *only* the type 
            sFindClass = type.substring(0, type.lastIndexOf(".")).trim();
            sFindName = type.substring(type.lastIndexOf(".") + 1).trim();
        } else {
            //No - Use the whole text as the type
            sFindName = type;
            sFindClass = parentClass;
        }

        //Try to find the type in our list of things...
        for (int iCount = 0; iCount < alItems.size(); iCount++) {
            objItem = (ItemData) alItems.toArray()[iCount];

            //If this is the correct type...
            if ((objItem.name.toUpperCase().trim().equals(sFindName.toUpperCase().trim()))
                    && (objItem.parentClass.toUpperCase().trim().equals(sFindClass.toUpperCase().trim()))) {
                //...store the hyperlink to it
                sPath = getChunkTypePath(objItem.chunkType);

                sLink = sPath + sFindName + Integer.toString(objItem.instance) + DefFileExt;

                break;
            }
        }

        //If we have a link, create the HTML...
        if (!sLink.trim().equals("")) {
            sResult = "<a href=\"" + sLink + "\">" + configModel.Namespace + sFindClass + "." + sFindName + "</a>";
        } else {
            //...otherwise use the *original* type data (as it'll contain any prefix text)
            sResult = type;
        }

        //Are we outputting the results as text?
        if (asText) {
            //Yes - Build up the type as a more 'text like' string
            //Do we have anything to add onto the start of the results (e.g. 'List<')?
            if (sPrepend.trim().equals("")) {
                //No - Just add on 'object' (e.g. 'MyType object')
                if (!sResult.trim().equals("")) {
                    if (!"void".equals(sResult.toLowerCase().trim())) {
                        sResult = sResult + " object";
                    }
                } else {
                    sResult = "";
                }
            } else {
                //Yes - Add on the leading text and a trailing '>'
                sPrepend = sPrepend.replace("&lt;", "");
                sResult = sPrepend.toLowerCase().trim() + " of " + sResult + " objects";
            }
        } else {
            //No - Just add on any leading and trailing data that we extracted
            sResult = sPrepend + sResult + sAppend;
        }

        //If we need to make the results italic...wrap them in HTML tags
        if (asItalic && (!sResult.trim().equals(""))) {
            sResult = "<i>" + sResult + "</i>";
        }

        return sResult;
    }

    /**
     * Given a chunk type, this routine works out which output path corresponds
     * to it
     *
     * @param chunkType
     * @return
     */
    private String getChunkTypePath(String chunkType) {
        String sResult = "";

        if (chunkType.equals(ctClass)) {
            sResult = "../Types/";
        }
        if (chunkType.equals(ctEnum)) {
            sResult = "../Enums/";
        }
        if (chunkType.equals(ctMethod)) {
            sResult = "../Services/";
        }
        if (chunkType.equals(ctTest)) {
            sResult = "../Tests/";
        }
        if (chunkType.equals(ctUnknown)) {
            sResult = "../Unknown/";
        }

        return sResult;
    }

    /**
     * Tests for a vowel
     *
     * @param sData
     * @return
     */
    private Boolean isVowel(String sData) {
        sData = sData.toUpperCase().trim();
        return (sData.startsWith("A") || sData.startsWith("E")) || ((sData.startsWith("I") || sData.startsWith("O")) || sData.startsWith("U"));
    }
}
