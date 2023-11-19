package structure.sectionpage;

import structure.CmColor;
import structure.HandleRef;

public class HeaderVariables {
    public int          lSizeInBits;                // R2007 Only
    public long         llRequiredVersions;         // R2013+
    // double           dUnknown; default value 412148564080.0
    // double           dUnknown; defualt value 1.0
    // double           dUnknown; defualt value 1.0
    // double           dUnknown; defualt value 1.0
    // String           tvUnknown; defualt ""
    // String           tvUnknown; defualt ""
    // String           tvUnknown; defualt ""
    // String           tvUnknown; defualt ""
    // int              lUnknown; default value 24L
    // int              lUnknown; default value 0L
    // short            sUnknown; default value 0   // R13-R14 Only
    public HandleRef    hCurrViewportEntityHeader;  // Pre-2004 Only:
    public boolean      bDimaso;
    public boolean      bDimsho;
    public boolean      bDimsav;    // Undocumented // R13-R14 Only
    public boolean      bPlinegen;
    public boolean      bOrthomode;
    public boolean      bRegenmode;
    public boolean      bFillmode;
    public boolean      bQtextmode;
    public boolean      bPsltscale;
    public boolean      bLimcheck;
    public boolean      bBlipmode;                  // R13-R14 Only
    public boolean      bUndocumented;              // R2004+
    public boolean      bUsrtimer;
    public boolean      bSkpoly;
    public boolean      bAngdir;
    public boolean      bSplframe;
    public boolean      bAttreq;                    // R13-R14 Only
    public boolean      bAttdia;                    // R13-R14 Only
    public boolean      bMirrtext;
    public boolean      bWorldview;
    public boolean      bWireframe; // Undocumented // R13-R14 Only
    public boolean      bTilemode;
    public boolean      bPlimcheck;
    public boolean      bVisretain;
    public boolean      bDelobj;                    // R13-R14 Only
    public boolean      bDispsilh;
    public boolean      bPellipse;  // not present in DXF
    public short        sProxygraphics;
    public short        sDragmode;                  // R13-R14 Only
    public short        sTreedepth;
    public short        sLunits;
    public short        sLuprec;
    public short        sAunits;
    public short        sAuprec;
    public short        sOsmode;                    // R13-R14 Only
    public short        sAttmode;
    public short        sCoords;                    // R13-R14 Only
    public short        sPdmode;
    public short        sPickstyle;                 // R13-R14 Only
    // int              lUnknown;                   // R2004+
    // int              lUnknown;                   // R2004+
    // int              lUnknown;                   // R2004+
    public short        sUseri1;
    public short        sUseri2;
    public short        sUseri3;
    public short        sUseri4;
    public short        sUseri5;
    public short        sSplinesegs;
    public short        sSurfu;
    public short        sSurfv;
    public short        sSurftype;
    public short        sSurftab1;
    public short        sSurftab2;
    public short        sSplinetype;
    public short        sShadedge;
    public short        sShadedif;
    public short        sUnitmode;
    public short        sMaxactvp;
    public short        sIsolines;
    public short        sCmljust;
    public short        sTextqlty;
    public double       dLtscale;
    public double       dTextsize;
    public double       dTracewid;
    public double       dSketchinc;
    public double       dFilletrad;
    public double       dThickness;
    public double       dAngbase;
    public double       dPdsize;
    public double       dPlinewid;
    public double       dUserr1;
    public double       dUserr2;
    public double       dUserr3;
    public double       dUserr4;
    public double       dUserr5;
    public double       dChamfera;
    public double       dChamferb;
    public double       dChamferc;
    public double       dChamferd;
    public double       dFacetres;
    public double       dCmlscale;
    public double       dCeltscale;
    public String       tMenuname;                  // R13-R18
    public int          lTdcreateJD;    // Julian day
    public int          lTdcreateMS;    // Milliseconds into the day
    public int          lTdupdateJD;    // Julian day
    public int          lTdupdateMS;    // Milliseconds into the day
    // int              lUnkndown;                  // R2004+
    // int              lUnknown                    // R2004+
    // int              lUnknown                    // R2004+
    public int          lTdindwgD;      // Days
    public int          lTdindwgMS;     // Milliseconds into the day
    public int          lTdusrtimerD;   // Days
    public int          lTdusrtimerMS;  // Milliseconds into the day
    public CmColor      cmCecolor;
    public HandleRef    hHandseed;      // The next handle
    public HandleRef    hClayer;
    public HandleRef    hTextstyle;
    public HandleRef    hCeltype;
    public HandleRef    hCmaterial;                 // R2007+ Only
    public HandleRef    hDimstyle;
    public HandleRef    hCmlstyle;
    public double       dPsvpscale;                 // R2000+ Only
    public double[]     dInsbasePspace;
    public double[]     dExtminPspace;
    public double[]     dExtmaxPspace;
    public double[]     dLimminPspace;
    public double[]     dLimmaxPspace;
    public double       dElevationPspace;
    public double[]     dUcsorgPspace;
    public double[]     dUcsxdirPspace;
    public double[]     dUcsydirPspace;
    public HandleRef    hUcsnamePspace;
    public HandleRef    hPucsorthoref;              // R2000+ Only
    public short        sPucsorthoview;             // R2000+ Only
    public HandleRef    hPucsbase;                  // R2000+ Only
    public double[]     dPucsorgtop;                // R2000+ Only
    public double[]     dPucsorgbottom;             // R2000+ Only
    public double[]     dPucsorgleft;               // R2000+ Only
    public double[]     dPucsorgright;              // R2000+ Only
    public double[]     dPucsorgfront;              // R2000+ Only
    public double[]     dPucsorgback;               // R2000+ Only
    public double[]     dInsbaseMspace;
    public double[]     dExtminMspace;
    public double[]     dExtmaxMspace;
    public double[]     dLimminMspace;
    public double[]     dLimmaxMspace;
    public double[]     dElevationMspace;
    public double[]     dUcsorgMspace;
    public double[]     dUcsxdirMspace;
    public double[]     dUcsydirMspace;
    public HandleRef    hUcsnameMspace;
    public HandleRef    hUcsorthoref;               // R2000+ Only
    public HandleRef    hUcsorthoview;              // R2000+ Only
    public HandleRef    hUcsbase;                   // R2000+ Only
    public double[]     dUcsorgtop;                 // R2000+ Only
    public double[]     dUcsorgbottom;              // R2000+ Only
    public double[]     dUcsorgleft;                // R2000+ Only
    public double[]     dUcsorgright;               // R2000+ Only
    public double[]     dUcsorgfront;               // R2000+ Only
    public double[]     dUcsorgback;                // R2000+ Only
    public String       tDimpost;                   // R2000+ Only
    public String       tDimapost;                  // R2000+ Only
    public boolean      bDimtol;                    // R13-R14 Only
    public boolean      bDimlim;                    // R13-R14 Only
    public boolean      bDimtih;                    // R13-R14 Only
    public boolean      bDimtoh;                    // R13-R14 Only
    public boolean      bDimse1;                    // R13-R14 Only
    public boolean      bDimse2;                    // R13-R14 Only
    public boolean      bDimalt;                    // R13-R14 Only
    public boolean      bDimtofl;                   // R13-R14 Only
    public boolean      bDimsah;                    // R13-R14 Only
    public boolean      bDimtix;                    // R13-R14 Only
    public boolean      bDimsoxd;                   // R13-R14 Only
    public byte         cDimaltd;                   // R13-R14 Only
    public byte         cDimzin;                    // R13-R14 Only
    public boolean      bDimsd1;                    // R13-R14 Only
    public boolean      bDimsd2;                    // R13-R14 Only
    public byte         cDimtolj;                   // R13-R14 Only
    public byte         cDimjust;                   // R13-R14 Only
    public byte         cDimfit;                    // R13-R14 Only
    public boolean      bDimupt;                    // R13-R14 Only
    public byte         cDimtzin;                   // R13-R14 Only
    public byte         cDimaltz;                   // R13-R14 Only
    public byte         cTimalttz;                  // R13-R14 Only
    public byte         cTimtad;                    // R13-R14 Only
    public short        sDimunit;                   // R13-R14 Only
    public short        sDimaunit;                  // R13-R14 Only
    public short        sDimdec;                    // R13-R14 Only
    public short        sDimtdec;                   // R13-R14 Only
    public short        sDimaltu;                   // R13-R14 Only
    public short        sDimalttd;                  // R13-R14 Only
    public HandleRef    hDimtxsty;                  // R13-R14 Only
    public double       dDimscale;
    public double       dDimasz;
    public double       dDimexo;
    public double       dDimdli;
    public double       dDimexe;
    public double       dDimrnd;
    public double       dDimdle;
    public double       dDimtp;
    public double       dDimtm;
    public double       dDimfxl;                    // R2007+ Only
    public double       dDimjogang;                 // R2007+ Only
    public short        sDimtfill;                  // R2007+ Only
    public CmColor      cmDimtfillclr;              // R2007+ Only
    // boolean          bDimtol;                    // R2000+ Only
    // boolean          bDimlim;                    // R2000+ Only
    // boolean          bDimtih;                    // R2000+ Only
    // boolean          bDimtoh;                    // R2000+ Only
    // boolean          bDimse1;                    // R2000+ Only
    // boolean          bDimse2;                    // R2000+ Only
    public short        sDimtad;                    // R2000+ Only
    public short        sDimzin;                    // R2000+ Only
    public short        sDimazin;                   // R2000+ Only
    public short        sDimarcsym;                 // R2007+ Only
    public double       dDimtxt;
    public double       dDimcen;
    public double       dDimtsz;
    public double       dDimaltf;
    public double       dDimlfac;
    public double       dDimtvp;
    public double       dDimtfac;
    public double       dDimgap;
    // String           tDimpost;                   // R13-R14 Only
    // String           tDimapost;                  // R13-R14 Only
    public String       tDimblk;                    // R13-R14 Only
    public String       tDimblk1;                   // R13-R14 Only
    public String       tDimblk2;                   // R13-R14 Only
    public double       dDimaltrnd;                 // R2000+ Only
    // boolean          bDimalt;                    // R2000+ Only
    public short        sDimaltd;                   // R2000+ Only
    // boolean          bDimtofl;                   // R2000+ Only
    // boolean          bDimsah;                    // R2000+ Only
    // boolean          bDimtix;                    // R2000+ Only
    // boolean          bDimsoxd;                   // R2000+ Only
    public CmColor      cmDimclrd;
    public CmColor      cmDimclre;
    public CmColor      cmDimclrt;
    public short        sDimadec;                   // R2000+ Only
    // short            sDimdec;                    // R2000+ Only
    // short            sDimtdec;                   // R2000+ Only
    // short            sDimaltu;                   // R2000+ Only
    // short            sDimalttd;                  // R2000+ Only
    // short            sDimaunit;                  // R2000+ Only
    public short        sDimfrac;                   // R2000+ Only
    public short        sDimlunit;                  // R2000+ Only
    public short        sDimdsep;                   // R2000+ Only
    public short        sDimtmove;                  // R2000+ Only
    public short        sDimjust;                   // R2000+ Only
    // boolean          bDimsd1;                    // R2000+ Only
    // boolean          bDimsd2;                    // R2000+ Only
    public short        sDimtolj;                   // R2000+ Only
    public short        sDimtzin;                   // R2000+ Only
    public short        sDimaltz;                   // R2000+ Only
    public short        sDimalttz;                  // R2000+ Only
    // boolean          bDimupt;                    // R2000+ Only
    public short        sDimatfit;                  // R2000+ Only
    public boolean      bDimfxlon;                  // R2007+ Only
    public boolean      bDimtxtdirection;           // R2010+ Only
    public double       dDimaltmzf;                 // R2010+ Only
    public String       tDimaltmzs;                 // R2010+ Only
    public double       dDimmzf;                    // R2010+ Only
    public String       tDimmzs;                    // R2010+ Only
    // HandleRef        hDimtxsty;                  // R2000+ Only
    public HandleRef    hDimldrblk;                 // R2000+ Only
    public HandleRef    hDimblk;                    // R2000+ Only
    public HandleRef    hDimblk1;                   // R2000+ Only
    public HandleRef    hDimblk2;                   // R2000+ Only
    public HandleRef    hDimltype;                  // R2007+ Only
    public HandleRef    hDimltex1;                  // R2007+ Only
    public HandleRef    hDimltex2;                  // R2007+ Only
    public short        sDimlwd;                    // R2000+ Only
    public short        sDimlwe;                    // R2000+ Only
    public HandleRef    hBlockCtrlObj;
    public HandleRef    hLayerCtrlObj;
    public HandleRef    hStyleCtrlObj;
    public HandleRef    hLinetypeCtrlObj;
    public HandleRef    hViewCtrlObj;
    public HandleRef    hUcsCtrlObj;
    public HandleRef    hVportCtrlObj;
    public HandleRef    hAppidCtrlObj;
    public HandleRef    hDimstyleCtrlObj;
    public HandleRef    hViewportEttyHdrCtrlObj;    // R13-R15 Only:
    public HandleRef    hDictionaryAcadGroup;
    public HandleRef    hDictionaryAcadMlinestyle;
    public HandleRef    hDictionaryNamedObjs;
    public short        sTstackalign; //default = 1 // R2000+ Only
    public short        sTstacksize;  //default = 70// R2000+ Only
    public String       tHyperlinkbase;             // R2000+ Only
    public String       tStylehseet;                // R2000+ Only
    public HandleRef    hDictionaryLayouts;         // R2000+ Only
    public HandleRef    hDictionaryPlotsettings;    // R2000+ Only
    public HandleRef    hDictionaryPlotstyles;      // R2000+ Only
    public HandleRef    hDictionaryMaterials;       // R2004+
    public HandleRef    hDictionaryColors;          // R2004+
    public HandleRef    hDictionaryVisualstyle;     // R2007+
    // HandleRef        hUnknown;                   // R2013+
    public int          lFlags;                     // R2000+
    public short        sInsunits;                  // R2000+
    public short        sCepsntype;                 // R2000+
    public HandleRef    hCpsnid;                    // R2000+
    public String       tFingerprintguid;           // R2000+
    public String       tVersionguid;               // R2000+
    public byte         cSortens;                   // R2004+
    public byte         cIndexctl;                  // R2004+
    public byte         cHidetext;                  // R2004+
    public byte         cXclipframe;                // R2004+
    public byte         cDimassoc;                  // R2004+
    public byte         cHalogap;                   // R2004+
    public short        sObjscuredcolor;            // R2004+
    public short        sIntersectioncolor;         // R2004+
    public byte         cObjscuredltype;            // R2004+
    public byte         cIntersectiondisplay;       // R2004+
    public String       tProjectname;               // R2004+
    public HandleRef    hBlockRecordPaperSpace;
    public HandleRef    hBlockRecordModelSpace;
    public HandleRef    hLtypeBylayer;
    public HandleRef    hLtypeByblock;
    public HandleRef    hLtypeContinuous;
    public boolean      bCameradisplay;             // R2007+
    // int              lUnknown;                   // R2007+
    // int              lUnknown                    // R2007+
    // double           dUnknown                    // R2007+
    public double       dStepspersec;               // R2007+
    public double       dStepsize;                  // R2007+
    public double       d3ddwfprec;                 // R2007+
    public double       dLenslength;                // R2007+
    public double       dCameraheight;              // R2007+
    public byte         cSolidhist;                 // R2007+
    public byte         cShowhist;                  // R2007+
    public double       dPsolwdith;                 // R2007+
    public double       dPsolheight;                // R2007+
    public double       dLoftang1;                  // R2007+
    public double       dLoftang2;                  // R2007+
    public double       dLoftmag1;                  // R2007+
    public double       dLoftmag2;                  // R2007+
    public short        sLoftparam;                 // R2007+
    public byte         cLoftnormals;               // R2007+
    public double       dLattide;                   // R2007+
    public double       dLongitude;                 // R2007+
    public double       dNorthdirection;            // R2007+
    public int          lTimezone;                  // R2007+
    public byte         cLightglyphdisplay;         // R2007+
    public byte         cTilemodelightsynch;        // R2007+
    public byte         cDwfframe;                  // R2007+
    public byte         cDgnframe;                  // R2007+
    // boolean          bUnknown                    // R2007+
    public CmColor      cmInterferecolor;           // R2007+
    public HandleRef    hInterfereobjvs;            // R2007+
    public HandleRef    hInterferevpvs;             // R2007+
    public HandleRef    hDragvs;                    // R2007+
    public byte         cCshadow;                   // R2007+
    // boolean          bUnknown;                   // R2007+
    // short            sUnknown; (type 5/6 only)   // R14+
    // short            sUnknown; (type 5/6 only)   // R14+
    // short            sUnknown; (type 5/6 only)   // R14+
    // short            sUnknown; (type 5/6 only)   // R14+

}
