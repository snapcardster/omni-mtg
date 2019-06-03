set d=C:\Arbeit\omni-mtg\server\

del /Q /F /S "%d%target\universal\omni-mtg"
del /Q /F /S "%d%target\universal\omni-mtg.zip"

mkdir "%d%target\temp"
del /Q /F /S "%d%target\temp"

REM "%JAVA_HOME%\bin\java.exe" -jar "C:/Users/Karsten/.IntelliJIdea2018.3/config/plugins/Scala/launcher/sbt-launch.jar" dist

Powershell.exe -executionpolicy remotesigned -File extract.ps1


mkdir "%d%target\temp\omnimtg-0.1-SNAPSHOT\omni-mtg-java-archive"

copy /Y "%d%omni-mtg-java-archive\omni-mtg.jar" "%d%target\temp\omnimtg-0.1-SNAPSHOT\omni-mtg-java-archive/omni-mtg.jar"

Powershell.exe -executionpolicy remotesigned -File zip.ps1