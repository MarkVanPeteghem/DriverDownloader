copy doc.txt dist
copy developer-doc.txt dist
copy *.xml dist
copy src\driverdownloader\*.* dist\src\driverdownloader
del dist\build*.xml
copy DriverDownloader.bat dist
copy DriverOverview.bat dist
del dist\README.TXT

rename dist DriverDownloader
del DriverDownloader.zip
7z a -tzip -r -mx=9 DriverDownloader.zip DriverDownloader\*
rename DriverDownloader dist

mkdir testzip
cd testzip
rm -rf *
7z x ..\DriverDownloader.zip
