Unicode true

!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "LogicLib.nsh"
!include "WinVer.nsh"

!define PRODUCT_NAME "NOS-Pipe"
!define PRODUCT_VERSION "1.0.2.2"
!define PRODUCT_PUBLISHER "NOS-Pipe project"
!define PRODUCT_WEB_SITE "https://samlib.ru/z/zwerew_m_a/"
!define PRODUCT_DIR_REGKEY "Software\NOS-Pipe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\NOS-Pipe"
; Path to the complete Windows payload containing the bundled JRE and MPlayer.
; Put it beside this source tree or replace this value with an absolute path.
!define SOURCE_DIR "..\..\NOS-Pipe-1.0.2.2-Windows"

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "output\NOS-Pipe-1.0.2.2-Setup.exe"
InstallDir "$PROGRAMFILES\NOS-Pipe"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" "Install_Dir"
RequestExecutionLevel admin
SetCompressor /SOLID lzma
SetCompressorDictSize 32
CRCCheck on
XPStyle on
BrandingText "NOS-Pipe 1.0.2.2"
Icon "assets\NOS-Pipe.ico"
UninstallIcon "assets\NOS-Pipe.ico"

VIProductVersion "1.0.2.2"
VIAddVersionKey /LANG=1033 "ProductName" "NOS-Pipe"
VIAddVersionKey /LANG=1033 "ProductVersion" "1.0.2.2"
VIAddVersionKey /LANG=1033 "FileDescription" "NOS-Pipe Setup"
VIAddVersionKey /LANG=1033 "FileVersion" "1.0.2.2"
VIAddVersionKey /LANG=1033 "LegalCopyright" "NOS-Pipe project, 2026"

!define MUI_ABORTWARNING
!define MUI_ICON "assets\NOS-Pipe.ico"
!define MUI_UNICON "assets\NOS-Pipe.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "assets\installer-sidebar.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "assets\installer-sidebar.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "assets\installer-header.bmp"
!define MUI_COMPONENTSPAGE_SMALLDESC

!define MUI_FINISHPAGE_RUN "$INSTDIR\NOS-Pipe.exe"
!define MUI_FINISHPAGE_RUN_TEXT $(FinishRun)
!define MUI_FINISHPAGE_SHOWREADME
!define MUI_FINISHPAGE_SHOWREADME_FUNCTION OpenLocalizedReadme
!define MUI_FINISHPAGE_SHOWREADME_TEXT $(FinishReadme)
!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_FINISHPAGE_LINK $(FinishAuthor)
!define MUI_FINISHPAGE_LINK_LOCATION "${PRODUCT_WEB_SITE}"

LicenseLangString LicenseFile 1049 "license-ru.txt"
LicenseLangString LicenseFile 1033 "license-en.txt"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE $(LicenseFile)
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

!insertmacro MUI_LANGUAGE "Russian"
!insertmacro MUI_LANGUAGE "English"

LangString CoreName ${LANG_RUSSIAN} "NOS-Pipe (обязательно)"
LangString CoreName ${LANG_ENGLISH} "NOS-Pipe (required)"
LangString CoreDesc ${LANG_RUSSIAN} "Программа, OpenJDK 7, MPlayer, документация и лицензии."
LangString CoreDesc ${LANG_ENGLISH} "Application, OpenJDK 7, MPlayer, documentation, and licenses."
LangString DesktopName ${LANG_RUSSIAN} "Ярлык на рабочем столе"
LangString DesktopName ${LANG_ENGLISH} "Desktop shortcut"
LangString DesktopDesc ${LANG_RUSSIAN} "Создать ярлык NOS-Pipe на рабочем столе для всех пользователей."
LangString DesktopDesc ${LANG_ENGLISH} "Create a NOS-Pipe desktop shortcut for all users."
LangString WallpaperName ${LANG_RUSSIAN} "Обои NOS-Pipe"
LangString WallpaperName ${LANG_ENGLISH} "NOS-Pipe wallpaper"
LangString WallpaperDesc ${LANG_RUSSIAN} "Создать обои точно под текущий экран, используя выбранный пользователем цвет рабочего стола."
LangString WallpaperDesc ${LANG_ENGLISH} "Create wallpaper for the current screen using the user's existing desktop color."
LangString FinishRun ${LANG_RUSSIAN} "Запустить NOS-Pipe"
LangString FinishRun ${LANG_ENGLISH} "Run NOS-Pipe"
LangString FinishReadme ${LANG_RUSSIAN} "Открыть README"
LangString FinishReadme ${LANG_ENGLISH} "Open README"
LangString FinishAuthor ${LANG_RUSSIAN} "Посетить страницу автора на Самиздате"
LangString FinishAuthor ${LANG_ENGLISH} "Visit the author's Samizdat page"

Section "$(CoreName)" SEC_CORE
  SectionIn RO
  SetShellVarContext all
  SetOutPath "$INSTDIR"
  File /r "${SOURCE_DIR}\*.*"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "Install_Dir" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\NOS-Pipe.exe" "" "$INSTDIR\NOS-Pipe.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\NOS-Pipe.exe" "Path" "$INSTDIR"

  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayName" "NOS-Pipe 1.0.2.2"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\NOS-Pipe.exe"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "DisplayVersion" "1.0.2.2"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "${PRODUCT_UNINST_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKLM "${PRODUCT_UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKLM "${PRODUCT_UNINST_KEY}" "NoRepair" 1
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  WriteRegDWORD HKLM "${PRODUCT_UNINST_KEY}" "EstimatedSize" $0

  CreateDirectory "$SMPROGRAMS\NOS-Pipe"
  CreateShortCut "$SMPROGRAMS\NOS-Pipe\NOS-Pipe.lnk" "$INSTDIR\NOS-Pipe.exe" "" "$INSTDIR\NOS-Pipe.exe" 0
  CreateShortCut "$SMPROGRAMS\NOS-Pipe\NOS-Pipe Wallpaper.lnk" "$INSTDIR\NOS-Pipe-Wallpaper.exe" "" "$INSTDIR\NOS-Pipe-Wallpaper.exe" 0
  StrCmp $LANGUAGE ${LANG_RUSSIAN} 0 +3
    CreateShortCut "$SMPROGRAMS\NOS-Pipe\README.lnk" "$INSTDIR\README-RU.txt"
    Goto +2
  CreateShortCut "$SMPROGRAMS\NOS-Pipe\README.lnk" "$INSTDIR\README-EN.txt"
  CreateShortCut "$SMPROGRAMS\NOS-Pipe\Удалить NOS-Pipe.lnk" "$INSTDIR\Uninstall.exe"
  WriteINIStr "$SMPROGRAMS\NOS-Pipe\Страница автора.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
SectionEnd

Section "$(DesktopName)" SEC_DESKTOP
  SetShellVarContext all
  CreateShortCut "$DESKTOP\NOS-Pipe.lnk" "$INSTDIR\NOS-Pipe.exe" "" "$INSTDIR\NOS-Pipe.exe" 0
SectionEnd

Section /o "$(WallpaperName)" SEC_WALLPAPER
  ; The native helper reads the real screen size and the user's current
  ; desktop colour, composites the original RGBA PNG and writes one exact-size
  ; BMP to the current user's APPDATA. It does not use Java or prepared images.
  ExecWait '"$INSTDIR\NOS-Pipe-Wallpaper.exe" /silent' $0
  ${If} $0 != 0
    DetailPrint "NOS-Pipe Wallpaper returned error $0. It can be retried from the Start menu."
  ${EndIf}
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_CORE} $(CoreDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DESKTOP} $(DesktopDesc)
  !insertmacro MUI_DESCRIPTION_TEXT ${SEC_WALLPAPER} $(WallpaperDesc)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Function .onInit
  !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function OpenLocalizedReadme
  StrCmp $LANGUAGE ${LANG_RUSSIAN} 0 +3
    ExecShell "open" "$INSTDIR\README-RU.txt"
    Return
  ExecShell "open" "$INSTDIR\README-EN.txt"
FunctionEnd

Section "Uninstall"
  SetShellVarContext current

  ReadRegStr $0 HKCU "Control Panel\Desktop" "Wallpaper"
  StrCmp $0 "$APPDATA\NOS-Pipe\NOS-Pipe-wallpaper.bmp" clear_wallpaper wallpaper_done
  clear_wallpaper:
    WriteRegStr HKCU "Control Panel\Desktop" "Wallpaper" ""
    System::Call 'user32::SystemParametersInfoW(i 20, i 0, w "", i 3)'
  wallpaper_done:
  Delete "$APPDATA\NOS-Pipe\NOS-Pipe-wallpaper.bmp"

  SetShellVarContext all
  Delete "$DESKTOP\NOS-Pipe.lnk"
  RMDir /r "$SMPROGRAMS\NOS-Pipe"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\App Paths\NOS-Pipe.exe"
  DeleteRegKey HKLM "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"

  RMDir /r "$INSTDIR"
SectionEnd
