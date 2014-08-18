/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package documenter.Services;

import documenter.Models.ConfigModel;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author John
 */
public class ConfigurationSvc {
public static ConfigModel Configuration;
    public static boolean IsDirty;
    //public static boolean HasChanged;
    public static String ConfigFilename;

    private static final String NewLine = "\n";
    
//    public static ConfigModel NewConfig() {
//        Configuration = new ConfigModel();
//        Configuration.MethodsTemplateFolder = "";
//        Configuration.UnknownTemplateFolder = "";
//        Configuration.ParamIndent = "  ";
//
//        Configuration.AlItems = new ArrayList<String>();
//        //this.edtDetail.Text = ""
//        Configuration.OutputFolder = "";
//        Configuration.TestTemplateFolder = "";
//        Configuration.EnumTemplateFolder = "";
//        Configuration.ClassTemplateFolder = "";
//        Configuration.InterfaceTemplateFolder = "";
//        Configuration.Namespace = "";
//        Configuration.ConfigurationFilename = "";
//        //Text = "FinancialForce.com Documentation Generator",;
//        Configuration.ShowGlobal = true;
//        Configuration.ShowPrivate = false;
//        Configuration.ShowPublic = false;
//        Configuration.ShowWebService = false;
//        Configuration.IncludeClasses = true;
//        Configuration.IncludeEnums = true;
//        Configuration.IncludeMethods = true;
//        Configuration.IncludeTests = false;
//        Configuration.IncludeUnknown = false;
//        Configuration.IncludeInterfaces = false;
//        Configuration.SkipRootClasses = true;
//        Configuration.SkipConstructor = false;
//        Configuration.Toc = 0;
//        Configuration.IncludeTocFullNames = false;
//        Configuration.TocPreNodes = "";
//        Configuration.TocPostNodes = "";
//        Configuration.AllExcludeFiles = new ArrayList<String>();
//        Configuration.ExcludeFiles = "";
//        Configuration.IncludeSnippets = true;
//        Configuration.SnippetMarker = "";
//        Configuration.HtmlPreEnumTable = "<table class=\"TableStyle-Fields\" style=\"margin-left: 0;margin-right: auto;mc-table-style: url('../../Resources/TableStyles/Fields.css');\" cellspacing=\"0\">\n<col class=\"Column-FieldName\" />\n<col class=\"Column-Column2\" />\n<thead>\n  <tr class=\"Head-Header1\">\n    <th class=\"HeadE-FieldName-Header1\">Value</th>\n    <th class=\"HeadD-Column2-Header1\">Description</th>\n  </tr>\n</thead>\n<tbody>".replace("\n", NewLine);;
//                        Configuration.HtmlLoopEnumTable = "<tr class=\"Body-Body1\" MadCap:conditions=\"\">\n  <td class=\"BodyE-FieldName-Body1\">[%ENUMVALUE%]</td>\n  <td class=\"BodyD-Column2-Body1\">&#160;</td>\n</tr>".replace("\n", NewLine);
//        Configuration.HtmlPostEnumTable = "</tbody>\n</table>".replace("\n", NewLine);
//        
//        Configuration.HtmlPreMethodList = "<ul>".replace("\n", NewLine);
//        Configuration.HtmlLoopMethodList = "<li><MadCap:xref href=\"[%ITEMLINK%]\">\"[%ITEMTYPE%]: [%ITEMNAME%]\"</MadCap:xref></li>".replace("\n", NewLine);
//        Configuration.HtmlPostMethodList = "</ul>".replace("\n", NewLine);
//        Configuration.HtmlPreParamTable = "<table class=\"TableStyle-Fields\" style=\"mc-table-style: url('../../Resources/TableStyles/Fields.css'); margin-left: 0; margin-right: auto;\" cellspacing=\"0\">\n<col class=\"Column-FieldName\">\n</col>\n<col class=\"Column-Column2\">\n</col>\n<col class=\"Column-Column2\">\n</col>\n  <thead>\n    <tr class=\"Head-Header1\">\n      <th class=\"HeadE-FieldName-Header1\">Name</th>\n      <th class=\"HeadE-Column2-Header1\">Type</th>\n      <th class=\"HeadD-Column2-Header1\">Description</th>\n    </tr>\n  </thead>\n<tbody>".replace("\n", NewLine);
//        Configuration.HtmlLoopParamTable = "<tr class=\"Body-Body1\">\n  <td class=\"BodyB-FieldName-Body1\">[%PARAMNAME%]</td>\n  <td class=\"BodyB-Column2-Body1\">[%PARAMTYPE%]</td>\n  <td class=\"BodyA-Column2-Body1\">&#160;</td>\n</tr>".replace("\n", NewLine);
//        Configuration.HtmlPostParamTable = "</tbody>\n</table>".replace("\n", NewLine);
//        Configuration.HtmlPrePropList = "<table class=\"TableStyle-Fields\" style=\"mc-table-style: url('../../Resources/TableStyles/Fields.css');margin-left: 0;margin-right: auto;\" cellspacing=\"0\">\n<col class=\"Column-FieldName\">\n</col>\n<col class=\"Column-Column2\">\n</col>\n<col class=\"Column-Column2\">\n</col>\n  <thead>\n    <tr class=\"Head-Header1\">\n      <th class=\"HeadE-FieldName-Header1\">Name</th>\n      <th class=\"HeadE-Column2-Header1\">Type</th>\n      <th class=\"HeadD-Column2-Header1\">Description</th>\n    </tr>\n  </thead>\n<tbody>".replace("\n", NewLine);
//        Configuration.HtmlLoopPropList = "<tr class=\"Body-Body1\">\n  <td class=\"BodyB-FieldName-Body1\">[%PROPERTYNAME%]</td>\n  <td class=\"BodyB-Column2-Body1\">[%PROPERTYTYPE%]</td>\n  <td class=\"BodyA-Column2-Body1\">&#160;</td>\n</tr>".replace("\n", NewLine);
//        Configuration.HtmlPostPropList = "</tbody>\n</table>".replace("\n", NewLine);
//        Configuration.HtmlPreInterfaces = "<ul>".replace("\n", NewLine);
//        Configuration.HtmlLoopInterfaces = "<li><MadCap:xref href=\"[%ITEMLINK%]\">\"[%TYPE%] [%NAME%]([%PARAMETERS%])\"</MadCap:xref></li>".replace("\n", NewLine);
//        Configuration.HtmlPostInterfaces = "</ul>".replace("\n", NewLine);
//        Configuration.SourceFolder = "";
//    
//        return Configuration ;
//}

        /// <summary>
/// Loads the configuration file.
/// </summary>
/// <param name="filename">The filename.</param>
/// <returns></returns>
public static ConfigModel LoadConfigFile(String filename)
        {
            ConfigFilename = filename;

            Configuration = new ConfigModel();
            int iNoLines = 0;
        String sReadData = "";
        String sTag = "";
        String value = "";
        String sCount = "";

        System.out.printf("Loading Configuration (%s)...", filename);

        if (filename.trim().equals("")) //Do we have a config file?
        {
            System.out.println("Abort");

            //Set the *global* abort...
//            sGlobalDebug = "No configuration file specified";
//            bGlobalAbort = true;

            return null;
        }

        //alItems.clear();
        
        BufferedReader inConfig;
        try {
        inConfig = new BufferedReader(new FileReader(filename));
            
            while ((sReadData = inConfig.readLine()) != null) {
                String[] strExcludeArray;
                if (sReadData.contains("=")) //Is the read line in the form <Tag>=<Value>?
                {									//Yes - We can process it then
                    //Split the data into <tag> and <Value>
                    sTag = sReadData.substring(0, sReadData.indexOf("=")).toUpperCase().trim();
                    value = sReadData.substring(sReadData.indexOf("=") + 1);

                    if (sTag.contains("[")) //Does the tag contain a 'multi-line' value?
                    { 							//Yes - Read in the multiple lines
                        sCount = sTag.substring(sTag.indexOf("[")).trim();
                        sTag = sTag.substring(0, sTag.indexOf("[")).trim();

                        sCount = sCount.replace("[", "").replace("]", "").trim();

                        iNoLines = Integer.parseInt(sCount);

                        for (int iCount = 1; iCount < iNoLines; iCount++) {
                            value = value + NewLine + inConfig.readLine();
                        }
                    }
                    
                    switch (sTag)
                    {
                        case "TEMPLATEUNKNOWN":
                            {
                                Configuration.UnknownTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATETEST":
                            {
                                Configuration.TestTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATEENUM":
                            {
                                Configuration.EnumTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATEMETHOD":
                            {
                                Configuration.MethodsTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATECLASS":
                            {
                                Configuration.ClassTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATEWEBSERVICES":
                            {
                                Configuration.WebServicesTemplateFolder = value;
                                continue;
                            }
                        case "TEMPLATEINTERFACES":
                            {
                                Configuration.InterfaceTemplateFolder = value;
                                continue;
                            }
                        case "PATHINPUT":
                            {
                                Configuration.SourceFolder = value;
                                continue;
                            }
                        case "PATHOUTPUT":
                            {
                                Configuration.OutputFolder = value;
                                continue;
                            }
                        case "FILEFILTER":
                            {
                                Configuration.FileFilter = value;
                                continue;
                            }
                        case "EXCLUDEFILES":
                            // TODO:                                edtExcludeFiles.Text = "";
                            strExcludeArray = value.split("\\|");
                            for(String file : strExcludeArray){
                                Configuration.AllExcludeFiles.add(file.toLowerCase());
                            }
                            continue;

                        case "OUTPUTCLASS":
                            {
                                Configuration.IncludeClasses = value.equals("1");
                                continue;
                            }
                        case "OUTPUTENUM":
                            {
                                Configuration.IncludeEnums = value.equals("1");
                                continue;
                            }
                        case "OUTPUTMETHOD":
                            {
                                Configuration.IncludeMethods = value.equals("1");
                                continue;
                            }
                        case "OUTPUTTEST":
                            {
                                Configuration.IncludeTests = value.equals("1");
                                continue;
                            }
                        case "OUTPUTUNKNOWN":
                            {
                                Configuration.IncludeUnknown = value.equals("1");
                                continue;
                            }
                        case "OUTPUTWEBSERVICES":
                            {
                                Configuration.IncludeWebServices = value.equals("1");
                                continue;
                            }
                        case "OUTPUTINTERFACES":
                            {
                                Configuration.IncludeInterfaces = value.equals("1");
                                continue;
                            }
                        case "TOC":
                            {
                                Configuration.Toc = Integer.parseInt(value);
                                continue;
                            }
                        case "TOCFULLNAMES":
                            {
                                Configuration.IncludeTocFullNames = value.equals("1");
                                continue;
                            }
                        case "TOCPRENODES":
                            {
                                Configuration.TocPreNodes = value;
                                continue;
                            }
                        case "TOCPOSTNODE":
                            {
                                Configuration.TocPostNodes = value;
                                continue;
                            }
                        case "SKIPROOT":
                            {
                                // This is confusing
                                Configuration.SkipRootClasses = value.equals("0");
                                continue;
                            }
                        case "SKIPCONSTRUCTORS":
                            {
                                //This is confusing
                                Configuration.SkipConstructor = value.equals("0");
                                continue;
                            }
                        case "OVERWRITE":
                            {
                                Configuration.bOverwriteDocs = value.equals("1");
                                continue;
                            }
                        case "SNIPPETS":
                            {
                                Configuration.IncludeSnippets = value.equals("1");
                                continue;
                            }
                        case "SNIPPETMARKER":
                            {
                                Configuration.SnippetMarker = value;
                                continue;
                            }
                        case "PARAMINDENT":
                            {
                                Configuration.ParamIndent = value.replace("\"", "");
                                continue;
                            }
                        case "NAMESPACE":
                            {
                                Configuration.Namespace = value;
                                continue;
                            }
                        case "SCOPEGLOBAL":
                            {
                                Configuration.ShowGlobal = value.equals("1");
                                continue;
                            }
                        case "SCOPEWEBSERVICE":
                            {
                                Configuration.ShowWebService = value.equals("1");
                                continue;
                            }
                        case "SCOPEPRIVATE":
                            {
                                Configuration.ShowPrivate = value.equals("1");
                                continue;
                            }
                        case "SCOPEPUBLIC":
                            {
                                Configuration.ShowPublic = value.equals("1");
                                continue;
                            }
                        case "HTMLPREENUMTABLE":
                            {
                                Configuration.HtmlPreEnumTable = value;
                                continue;
                            }
                        case "HTMLLOOPENUMTABLE":
                            {
                                Configuration.HtmlLoopEnumTable = value;
                                continue;
                            }
                        case "HTMLPOSTENUMTABLE":
                            {
                                Configuration.HtmlPostEnumTable = value;
                                continue;
                            }
                        case "HTMLPREMETHODLIST":
                            {
                                Configuration.HtmlPreMethodList = value;
                                continue;
                            }
                        case "HTMLLOOPMETHODLIST":
                            {
                                Configuration.HtmlLoopMethodList = value;
                                continue;
                            }
                        case "HTMLPOSTMETHODLIST":
                            {
                                Configuration.HtmlPostMethodList = value;
                                continue;
                            }
                        case "HTMLPREPARAMTABLE":
                            {
                                Configuration.HtmlPreParamTable = value;
                                continue;
                            }
                        case "HTMLLOOPPARAMTABLE":
                            {
                                Configuration.HtmlLoopParamTable = value;
                                continue;
                            }
                        case "HTMLPOSTPARAMTABLE":
                            {
                                Configuration.HtmlPostParamTable = value;
                                continue;
                            }
                        case "HTMLPREPROPLIST":
                            {
                                Configuration.HtmlPrePropList = value;
                                continue;
                            }
                        case "HTMLLOOPPROPLIST":
                            {
                                Configuration.HtmlLoopPropList = value;
                                continue;
                            }
                        case "HTMLPOSTPROPLIST":
                            {
                                Configuration.HtmlPostPropList = value;
                                continue;
                            }
                        case "HTMLPREINTERFACES":
                            {
                                Configuration.HtmlPreInterfaces = value;
                                continue;
                            }
                        case "HTMLLOOPINTERFACES":
                            {
                                Configuration.HtmlLoopInterfaces = value;
                                continue;
                            }
                        case "HTMLPOSTINTERFACES":
                            {
                                Configuration.HtmlPostInterfaces = value;
                                continue;
                            }
                    }
                }
//                continue;
//            Label_05B4:
//                if (strExcludeArray[num3].Trim() != "")
//                {
//                    Configuration.AllExcludeFiles.Add(strExcludeArray[num3]);
//                    if (Configuration.ExcludeFiles == "")
//                    {
//                        Configuration.ExcludeFiles = strExcludeArray[num3];
//                    }
//                    else
//                    {
//                        Configuration.ExcludeFiles = Configuration.ExcludeFiles + ", " + strExcludeArray[num3];
//                    }
//                }
//                num3++;
//            Label_062F:
//                if (num3 < strExcludeArray.Length)
//                {
//                    goto Label_05B4;
//                }
            }

            //All done, close the file 
            inConfig.close();

            System.out.println("Done");
        } catch (FileNotFoundException e) {
            //sGlobalDebug = "FileNotFound (Config): " + sConfigFile + " Error: " + e.getMessage();
            //bGlobalAbort = true;
        } catch (IOException e) {
            //sGlobalDebug = "IOException (Config): " + sConfigFile + " Error: " + e.getMessage();
//            bGlobalAbort = true;
        }
        
        if(Configuration.AlHtml != null){
            
            Configuration.AlHtml.add(Configuration.HtmlPreEnumTable);
            Configuration.AlHtml.add(Configuration.HtmlLoopEnumTable);
            Configuration.AlHtml.add(Configuration.HtmlPostEnumTable);
            Configuration.AlHtml.add(Configuration.HtmlPreMethodList);
            Configuration.AlHtml.add(Configuration.HtmlLoopMethodList);
            Configuration.AlHtml.add(Configuration.HtmlPostMethodList);
            Configuration.AlHtml.add(Configuration.HtmlPreParamTable);
            Configuration.AlHtml.add(Configuration.HtmlLoopParamTable);
            Configuration.AlHtml.add(Configuration.HtmlPostParamTable);
            Configuration.AlHtml.add(Configuration.HtmlPrePropList);
            Configuration.AlHtml.add(Configuration.HtmlLoopPropList);
            Configuration.AlHtml.add(Configuration.HtmlPostPropList);
            Configuration.AlHtml.add(Configuration.HtmlPreInterfaces);
            Configuration.AlHtml.add(Configuration.HtmlLoopInterfaces);
            Configuration.AlHtml.add(Configuration.HtmlPostInterfaces);
        }
 //           reader.Close();
            
            return Configuration;
        }

        //public static void Save(ConfigModel configModel)
        //{
            //string filename = string.Format(@"{0}\{1}_{2}.dcf", Path.GetDirectoryName(ConfigFilename), Path.GetFileNameWithoutExtension(ConfigFilename), DateTime.Now.ToString("ddMMyyyy_HHmmss"));

          //  SaveData(ConfigFilename, configModel);

            //HasChanged = true;
            //string str = "";
            //var dlg = new SaveFileDialog();
            //try
            //{
            //    dlg.FileName = ConfigFilename;
            //    if (dlg.ShowDialog() == true)
            //    {
            //        //ConfigurationFilename = dlg.FileName;

            //        string filename = string.Format(@"C:\Documenter\Templates\Test\{0}_{1}.dcf", Path.GetFileNameWithoutExtension(ConfigFilename), DateTime.Now.ToString("ddMMyyyy_HHmmss"));
            //        var sw = new StreamWriter(filename);
            //        sw.WriteLine("TemplateUnknown=" + confimodel.UnknownTemplateFolder);
            //        sw.WriteLine("TemplateTest=" + confimodel.TestTemplateFolder);
            //        sw.WriteLine("TemplateEnum=" + confimodel.EnumTemplateFolder);
            //        sw.WriteLine("TemplateMethod=" + confimodel.MethodsTemplateFolder);
            //        sw.WriteLine("TemplateClass=" + confimodel.ClassTemplateFolder);
            //        sw.WriteLine("TemplateWebServices=" + confimodel.WebServicesTemplateFolder);
            //        sw.WriteLine("TemplateInterfaces=" + confimodel.InterfaceTemplateFolder);
            //        sw.WriteLine("OutputClass=" + BoolToStr(confimodel.IncludeClasses));
            //        sw.WriteLine("OutputEnum=" + BoolToStr(confimodel.IncludeEnums));
            //        sw.WriteLine("OutputMethod=" + BoolToStr(confimodel.IncludeMethods));
            //        sw.WriteLine("OutputTest=" + BoolToStr(confimodel.IncludeTests));
            //        sw.WriteLine("OutputUnknown=" + BoolToStr(confimodel.IncludeUnknown));
            //        sw.WriteLine("OutputWebServices=" + BoolToStr(confimodel.IncludeWebServices));
            //        sw.WriteLine("OutputInterfaces=" + BoolToStr(confimodel.IncludeInterfaces));
            //        sw.WriteLine("PathInput=" + confimodel.SourceFolder);
            //        sw.WriteLine("PathOutput=" + confimodel.OutputFolder);
            //        sw.WriteLine("Namespace=" + confimodel.Namespace);
            //        sw.WriteLine("ParamIndent=\"" + confimodel.ParamIndent + "\"");
            //        sw.WriteLine("TOC=" + confimodel.Toc.ToString(CultureInfo.InvariantCulture));
            //        sw.WriteLine("TOCFullNames=" + BoolToStr(confimodel.bTOCFullNames));
            //        GenerateSaveScript(sw, "TOCPreNodes", confimodel.sTOCPreNodes);


            //        if (string.IsNullOrEmpty(confimodel.sTOCPostNodes))
            //            confimodel.sTOCPostNodes = "JOHN";
            //        GenerateSaveScript(sw, "TOCPostNode", confimodel.sTOCPostNodes);

            //        sw.WriteLine("SkipRoot=" + BoolToStr(confimodel.SkipRootClasses));
            //        sw.WriteLine("SkipConstructors=" + BoolToStr(confimodel.SkipConstructor));
            //        sw.WriteLine("ScopeGlobal=" + BoolToStr(confimodel.ShowGlobal));
            //        sw.WriteLine("ScopeWebService=" + BoolToStr(confimodel.ShowWebService));
            //        sw.WriteLine("ScopePrivate=" + BoolToStr(confimodel.ShowPrivate));
            //        sw.WriteLine("ScopePublic=" + BoolToStr(confimodel.ShowPublic));
            //        sw.WriteLine("Overwrite=" + BoolToStr(confimodel.bOverwriteDocs));
            //        sw.WriteLine("Snippets=" + BoolToStr(confimodel.bSnippets));
            //        sw.WriteLine("SnippetMarker=" + confimodel.SnippetMarker);
            //        GenerateSaveScript(sw, "HTMLPreEnumTable", confimodel.HtmlPreEnumTable);
            //        GenerateSaveScript(sw, "HTMLLoopEnumTable", confimodel.HtmlLoopEnumTable);
            //        GenerateSaveScript(sw, "HTMLPostEnumTable", confimodel.HtmlPostEnumTable);
            //        GenerateSaveScript(sw, "HTMLPreMethodList", confimodel.HtmlPreMethodList);
            //        GenerateSaveScript(sw, "HTMLLoopMethodList", confimodel.HtmlLoopMethodList);
            //        GenerateSaveScript(sw, "HTMLPostMethodList", confimodel.HtmlPostMethodList);
            //        GenerateSaveScript(sw, "HTMLPreParamTable", confimodel.HtmlPreParamTable);
            //        GenerateSaveScript(sw, "HTMLLoopParamTable", confimodel.HtmlLoopParamTable);
            //        GenerateSaveScript(sw, "HTMLPostParamTable", confimodel.HtmlPostParamTable);
            //        GenerateSaveScript(sw, "HTMLPrePropList", confimodel.HtmlPrePropList);
            //        GenerateSaveScript(sw, "HTMLLoopPropList", confimodel.HtmlLoopPropList);
            //        GenerateSaveScript(sw, "HTMLPostPropList", confimodel.HtmlPostPropList);
            //        GenerateSaveScript(sw, "HTMLPreInterfaces", confimodel.HtmlPreInterfaces);
            //        GenerateSaveScript(sw, "HTMLLoopInterfaces", confimodel.HtmlLoopInterfaces);
            //        GenerateSaveScript(sw, "HTMLPostInterfaces", confimodel.HtmlPostInterfaces);
            //        for (int i = 0; i < confimodel.AlExcludeFiles.Count; i++)
            //        {
            //            str = str + confimodel.AlExcludeFiles[i].ToString() + "|";
            //        }
            //        sw.WriteLine("ExcludeFiles=" + str);
            //        sw.Close();
            //        //if (sConfigFile.Contains(@"\"))
            //        //{
            //        //    Text = "FinancialForce.com Documentation Generator - " +
            //        //                sConfigFile.Substring(sConfigFile.LastIndexOf(@"\") + 1).Trim();
            //        //}
            //        //else
            //        //{
            //        //    this.Text = "FinancialForce.com Documentation Generator";
            //        //}
            //        IsDirty = false;

            //    }
            //}
            //catch (Exception ex)
            //{
            //    System.Diagnostics.Debug.WriteLine(ex.Message);
            //}
            //finally
            //{
            //    //((IDisposable)dlg).Dispose();
            //}
        //}

        //public static void SaveAs(string filename, ConfigModel configModel)
        //{
            //var dlg = new SaveFileDialog();
            //try
            //{
            //    dlg.FileName = ConfigFilename;
            //    if (dlg.ShowDialog() == true)
            //    {
            //        SaveData(filename, configModel);

            //        IsDirty = false;
            //        HasChanged = true;
            //    }
            //}
            //SaveData(filename, configModel);
            //HasChanged = true;
        //}

//        private static void SaveData(string filename, ConfigModel configModel)
//        {
//            var sw = new StreamWriter(filename);
//            sw.WriteLine("TemplateUnknown=" + configModel.UnknownTemplateFolder);
//            sw.WriteLine("TemplateTest=" + configModel.TestTemplateFolder);
//            sw.WriteLine("TemplateEnum=" + configModel.EnumTemplateFolder);
//            sw.WriteLine("TemplateMethod=" + configModel.MethodsTemplateFolder);
//            sw.WriteLine("TemplateClass=" + configModel.ClassTemplateFolder);
//            sw.WriteLine("TemplateWebServices=" + configModel.WebServicesTemplateFolder);
//            sw.WriteLine("TemplateInterfaces=" + configModel.InterfaceTemplateFolder);
//            sw.WriteLine("OutputClass=" + BoolToStr(configModel.IncludeClasses));
//            sw.WriteLine("OutputEnum=" + BoolToStr(configModel.IncludeEnums));
//            sw.WriteLine("OutputMethod=" + BoolToStr(configModel.IncludeMethods));
//            sw.WriteLine("OutputTest=" + BoolToStr(configModel.IncludeTests));
//            sw.WriteLine("OutputUnknown=" + BoolToStr(configModel.IncludeUnknown));
//            sw.WriteLine("OutputWebServices=" + BoolToStr(configModel.IncludeWebServices));
//            sw.WriteLine("OutputInterfaces=" + BoolToStr(configModel.IncludeInterfaces));
//            sw.WriteLine("PathInput=" + configModel.SourceFolder);
//            sw.WriteLine("PathOutput=" + configModel.OutputFolder);
//            sw.WriteLine("FileFilter=" + configModel.FileFilter);
//            sw.WriteLine("Namespace=" + configModel.Namespace);
//            sw.WriteLine("ParamIndent=\"" + configModel.ParamIndent + "\"");
//            sw.WriteLine("TOC=" + configModel.Toc.ToString(CultureInfo.InvariantCulture));
//            sw.WriteLine("TOCFullNames=" + BoolToStr(configModel.IncludeTocFullNames));
//            GenerateSaveScript(sw, "TOCPreNodes", configModel.TocPreNodes);
//            GenerateSaveScript(sw, "TOCPostNode", configModel.TocPostNodes);
//            sw.WriteLine("SkipRoot=" + BoolToStr( false == configModel.SkipRootClasses));
//            sw.WriteLine("SkipConstructors=" + BoolToStr( false == configModel.SkipConstructor));
//            sw.WriteLine("ScopeGlobal=" + BoolToStr(configModel.ShowGlobal));
//            sw.WriteLine("ScopeWebService=" + BoolToStr(configModel.ShowWebService));
//            sw.WriteLine("ScopePrivate=" + BoolToStr(configModel.ShowPrivate));
//            sw.WriteLine("ScopePublic=" + BoolToStr(configModel.ShowPublic));
//            sw.WriteLine("Overwrite=" + BoolToStr(configModel.bOverwriteDocs));
//            sw.WriteLine("Snippets=" + BoolToStr(configModel.IncludeSnippets));
//            sw.WriteLine("SnippetMarker=" + configModel.SnippetMarker);
//            GenerateSaveScript(sw, "HTMLPreEnumTable", configModel.HtmlPreEnumTable);
//            GenerateSaveScript(sw, "HTMLLoopEnumTable", configModel.HtmlLoopEnumTable);
//            GenerateSaveScript(sw, "HTMLPostEnumTable", configModel.HtmlPostEnumTable);
//            GenerateSaveScript(sw, "HTMLPreMethodList", configModel.HtmlPreMethodList);
//            GenerateSaveScript(sw, "HTMLLoopMethodList", configModel.HtmlLoopMethodList);
//            GenerateSaveScript(sw, "HTMLPostMethodList", configModel.HtmlPostMethodList);
//            GenerateSaveScript(sw, "HTMLPreParamTable", configModel.HtmlPreParamTable);
//            GenerateSaveScript(sw, "HTMLLoopParamTable", configModel.HtmlLoopParamTable);
//            GenerateSaveScript(sw, "HTMLPostParamTable", configModel.HtmlPostParamTable);
//            GenerateSaveScript(sw, "HTMLPrePropList", configModel.HtmlPrePropList);
//            GenerateSaveScript(sw, "HTMLLoopPropList", configModel.HtmlLoopPropList);
//            GenerateSaveScript(sw, "HTMLPostPropList", configModel.HtmlPostPropList);
//            GenerateSaveScript(sw, "HTMLPreInterfaces", configModel.HtmlPreInterfaces);
//            GenerateSaveScript(sw, "HTMLLoopInterfaces", configModel.HtmlLoopInterfaces);
//            GenerateSaveScript(sw, "HTMLPostInterfaces", configModel.HtmlPostInterfaces);
//
//            string str = configModel.AllExcludeFiles.Aggregate("", (current, t) => current + t + "|");
//
//            sw.WriteLine("ExcludeFiles=" + str);
//            sw.Close();
//            //if (sConfigFile.Contains(@"\"))
//            //{
//            //    Text = "FinancialForce.com Documentation Generator - " +
//            //                sConfigFile.Substring(sConfigFile.LastIndexOf(@"\") + 1).Trim();
//            //}
//            //else
//            //{
//            //    this.Text = "FinancialForce.com Documentation Generator";
//            //}
//            IsDirty = false;
//        }
         

        public static ConfigModel GetConfiguration()
        {
            return Configuration;
        }

        /// <summary>
        /// Generates the save script.
        /// </summary>
        /// <param name="writer">The writer.</param>
        /// <param name="tag">The tag.</param>
        /// <param name="data">The data.</param>
//        private static void GenerateSaveScript(StreamWriter writer, string tag, string data)
//        {
//            var strArray = data.Split('\n');
//            if (strArray.Length != 0)
//            {
//                if (strArray.Length == 1)
//                {
//                    writer.WriteLine(tag + "=" + strArray[0].Trim());
//                }
//                else
//                {
//                    var strArray2 = new[] { tag, "[", strArray.Length.ToString(CultureInfo.InvariantCulture), "]=", strArray[0].Trim() };
//                    writer.WriteLine(string.Concat(strArray2));
//                    for (int i = 1; i < strArray.Length; i++)
//                    {
//                        writer.WriteLine(strArray[i].Trim());
//                    }
//                }
//            }
//        }

        /// <summary>
        /// Bools to string.
        /// </summary>
        /// <param name="value">if set to <c>true</c> [value].</param>
        /// <returns></returns>
//        private static string BoolToStr(boolean value)
//        {
//            return value ? "1" : "0";
//        }    
}
