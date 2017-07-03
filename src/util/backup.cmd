@Echo Off

rem if EXIST backup.run (echo "Another instance running. Program will exit." && exit 4) else (date /t > backup.run)
taskkill /F /IM ncftpput.exe
ping 127.0.0.1

set SESSION=backup-host
set TERMINAL_NUMBER=%1
set CLIENT_NAME=%2
set FTP_PASSWORD=%3
set LOGBIN_PATH=%4
set LOGBIN_BACKUP_PATH=%LOGBIN_PATH%-backup
set LOGBIN_TRANSFER_PATH=%LOGBIN_PATH%-transfer
set BACKUP_FILE_NAME=%TERMINAL_NUMBER%-%5

echo %date% - %time%

if "%CLIENT_NAME%" == "" (
    echo Environment variable CLIENT_NAME doesn't exist. Please update variable.
    exit 1
)
if "%LOGBIN_PATH%" == "" (
    echo Environment variable LOGBIN_PATH doesn't exist. Please update variable.
    exit 2
)

if NOT EXIST %LOGBIN_PATH% echo "Create %LOGBIN_PATH% files" && mkdir %LOGBIN_PATH%
if NOT EXIST %LOGBIN_BACKUP_PATH% echo "Create %LOGBIN_BACKUP_PATH% folder" && mkdir %LOGBIN_BACKUP_PATH%
if NOT EXIST %LOGBIN_TRANSFER_PATH% echo "Create %LOGBIN_TRANSFER_PATH% folder" && mkdir %LOGBIN_TRANSFER_PATH%

echo Creating archive of all zip files.
if EXIST %LOGBIN_PATH%\fulldump*.zip (
    echo Full dump found. Creating archive.
    rar -m0 -y mf %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME% %LOGBIN_PATH%\fulldump*.zip %LOGBIN_PATH%\system*.zip
    if ERRORLEVEL 0 (
        echo Creating md5 sum of full dump archive.
        md5.exe %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.rar > %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.md5
        copy /y %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.md5 %LOGBIN_BACKUP_PATH%\
        copy /y %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.rar %LOGBIN_BACKUP_PATH%\
        move /y %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.md5 %LOGBIN_TRANSFER_PATH%\
        move /y %LOGBIN_PATH%\fulldump-%BACKUP_FILE_NAME%.rar %LOGBIN_TRANSFER_PATH%\
    )
)

echo Creating archive of all completed binary logs.
rar -m5 -y -x%LOGBIN_PATH%\*.index mf %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME% %LOGBIN_PATH%\logg*.*
if EXIST %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.rar (
    echo Creating md5 sum of binary logs archive.
    md5.exe %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.rar > %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.md5
    copy /y %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.md5 %LOGBIN_BACKUP_PATH%\
    copy /y %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.rar %LOGBIN_BACKUP_PATH%\
    move /y %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.md5 %LOGBIN_TRANSFER_PATH%\
    move /y %LOGBIN_PATH%\inc-%BACKUP_FILE_NAME%.rar %LOGBIN_TRANSFER_PATH%\
)

if EXIST %LOGBIN_TRANSFER_PATH%\*.rar goto LABEL_UPLOAD
else  goto LABEL_EXIT

:LABEL_UPLOAD
echo Label Upload
echo Tranferring md5 files to ftp 192.168.55.13 into directory %TERMINAL_NUMBER%
rem ncftpput -u %CLIENT_NAME% -p %FTP_PASSWORD% -d ncftpput.log -m -v -z -F -DD 192.168.55.13 %TERMINAL_NUMBER% %LOGBIN_TRANSFER_PATH%\*.md5
ncftpput -u %CLIENT_NAME% -p %FTP_PASSWORD% -m -V -z -F -DD 192.168.55.13 %TERMINAL_NUMBER% %LOGBIN_TRANSFER_PATH%\*.md5
echo Tranferring inc files to ftp 192.168.55.13 into directory %TERMINAL_NUMBER%
ncftpput -u %CLIENT_NAME% -p %FTP_PASSWORD% -m -V -z -F -DD 192.168.55.13 %TERMINAL_NUMBER% %LOGBIN_TRANSFER_PATH%\inc*.rar
echo Tranferring fuldump files to ftp 192.168.55.13 into directory %TERMINAL_NUMBER%
ncftpput -u %CLIENT_NAME% -p %FTP_PASSWORD% -m -V -z -F -DD 192.168.55.13 %TERMINAL_NUMBER% %LOGBIN_TRANSFER_PATH%\fulldump*.rar
goto LABEL_EXIT

:LABEL_ERROR
echo Label Error
echo "Error has been detected. Exiting..."

:LABEL_EXIT
echo 
echo Label Exit
rem del /q backup.run
exit 0
