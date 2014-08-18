/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documenter.Models;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author John
 */
public class ConfigModel {

    public boolean IsDirty;

    public List<String> AlItems = new ArrayList<String>();
    public List<String> AlHtml = new ArrayList<String>();
    public List<String> AllExcludeFiles = new ArrayList<String>();
    public List<String> SnippetsList = new ArrayList<String>();
    public ArrayList AlItemUsage = new ArrayList();
    //private string sFolder;

    public String HtmlLoopEnumTable = "";
    public String HtmlLoopInterfaces = "";
    public String HtmlLoopMethodList = "";
    public String HtmlLoopParamTable = "";
    public String HtmlLoopPropList = "";
    public String HtmlPostEnumTable = "";
    public String HtmlPostInterfaces = "";
    public String HtmlPostMethodList = "";
    public String HtmlPostParamTable = "";
    public String HtmlPostPropList = "";
    public String HtmlPreEnumTable = "";
    public String HtmlPreInterfaces = "";
    public String HtmlPreMethodList = "";
    public String HtmlPreParamTable = "";
    public String HtmlPrePropList = "";

    public boolean IncludeClasses;
    public boolean IncludeEnums;
    public boolean IncludeInterfaces;
    public boolean IncludeMethods;
    public boolean IncludeTests;
    public boolean IncludeWebServices;
    public boolean IncludeUnknown;
    public boolean IncludeTocFullNames;
    public boolean bOverwriteDocs;
    public boolean IncludeSnippets;
    public boolean SkipRootClasses;
    public boolean SkipConstructor;
    //public boolean OverwriteDocs;
    public boolean ShowGlobal;
    public boolean ShowWebService;
    public boolean ShowPublic;
    public boolean ShowPrivate;
    public String ConfigurationFilename;
    public String SourceFolder;
    public String OutputFolder;
    public String ExcludeFiles;
    public String ClassTemplateFolder;
    public String EnumTemplateFolder;
    public String InterfaceTemplateFolder;
    public String MethodsTemplateFolder;
    public String TestTemplateFolder;
    public String WebServicesTemplateFolder;
    public String UnknownTemplateFolder;
    public String SnippetMarker;
    public String Namespace;
    public String FileFilter;
    public int Toc;
    public String ParamIndent;
    public String TocPreNodes;
    public String TocPostNodes;
}
