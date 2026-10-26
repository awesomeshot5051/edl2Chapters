; Edl2Chapters Windows installer (NSIS). Built by `gradlew packageExe` with makensis.
; Per-user install (no admin rights), start menu + desktop shortcut, and `edl2chapters` added to the user's PATH.
; Defines passed by Gradle: APP_VERSION, APP_DIR (jpackage app-image), ICON_FILE, OUT_FILE.

Unicode true
!include "MUI2.nsh"
!include "StrFunc.nsh"
!include "WinMessages.nsh"
${StrStr}
${UnStrRep}

!define APP_NAME "Edl2Chapters"
!define UNINSTALL_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"
!define ENV_KEY "Environment"

Name "${APP_NAME} ${APP_VERSION}"
OutFile "${OUT_FILE}"
InstallDir "$LOCALAPPDATA\Programs\${APP_NAME}"
InstallDirRegKey HKCU "Software\${APP_NAME}" "InstallDir"
RequestExecutionLevel user
SetCompressor /SOLID lzma

!define MUI_ICON "${ICON_FILE}"
!define MUI_UNICON "${ICON_FILE}"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

; Appends $INSTDIR\cli to the user's PATH unless already there.
Function AddToPath
  ReadRegStr $0 HKCU "${ENV_KEY}" "Path"
  ${StrStr} $1 ";$0;" ";$INSTDIR\cli;"
  StrCmp $1 "" 0 done
  StrCmp $0 "" 0 +3
    StrCpy $0 "$INSTDIR\cli"
    Goto write
  StrCpy $0 "$0;$INSTDIR\cli"
  write:
  WriteRegExpandStr HKCU "${ENV_KEY}" "Path" "$0"
  SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} 0 "STR:Environment" /TIMEOUT=5000
  done:
FunctionEnd

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "${APP_DIR}\*.*"

  ; `edl2chapters file.edl` in any terminal: a shim that calls the console launcher.
  CreateDirectory "$INSTDIR\cli"
  FileOpen $0 "$INSTDIR\cli\edl2chapters.cmd" w
  FileWrite $0 '@"%~dp0..\edl2chapters-cli.exe" %*$\r$\n'
  FileClose $0
  Call AddToPath

  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" "$INSTDIR\${APP_NAME}.exe"
  CreateShortcut "$DESKTOP\${APP_NAME}.lnk" "$INSTDIR\${APP_NAME}.exe"

  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\${APP_NAME}" "InstallDir" "$INSTDIR"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayName" "${APP_NAME}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "Publisher" "awesomeshot5051"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayIcon" "$INSTDIR\${APP_NAME}.exe"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoModify" 1
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoRepair" 1
SectionEnd

Section "Uninstall"
  ; Take our entry back out of PATH.
  ReadRegStr $0 HKCU "${ENV_KEY}" "Path"
  ${UnStrRep} $1 "$0" ";$INSTDIR\cli" ""
  ${UnStrRep} $1 "$1" "$INSTDIR\cli;" ""
  ${UnStrRep} $1 "$1" "$INSTDIR\cli" ""
  WriteRegExpandStr HKCU "${ENV_KEY}" "Path" "$1"
  SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} 0 "STR:Environment" /TIMEOUT=5000

  Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
  RMDir "$SMPROGRAMS\${APP_NAME}"
  Delete "$DESKTOP\${APP_NAME}.lnk"
  RMDir /r "$INSTDIR"
  DeleteRegKey HKCU "${UNINSTALL_KEY}"
  DeleteRegKey HKCU "Software\${APP_NAME}"
SectionEnd
