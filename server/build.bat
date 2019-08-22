set d=D:/Arbeit/omni-mtg/server/
set usrScala=C:/Users/Karsten/.IdeaIC2017.3/

del /Q /F /S "%d%target\universal\omni-mtg"
del /Q /F /S "%d%target\universal\omni-mtg.zip"

mkdir "%d%target\temp"
del /Q /F /S "%d%target\temp"
del /Q /F /S "%d%target\temp\omni-mtg"
rmdir /Q /S "%d%target\temp\omni-mtg"
rmdir /Q /S "%d%target\temp"

"%JAVA_HOME%\bin\java.exe" -jar "%usrScala%config/plugins/Scala/launcher/sbt-launch.jar" dist

Powershell.exe -executionpolicy remotesigned -File extract.ps1

move /Y "%d%target\temp\omnimtg-0.1-SNAPSHOT" "%d%target\temp\omni-mtg"

mkdir "%d%target\temp\omni-mtg\omni-mtg-java-archive"

copy /Y "%d%omni-mtg-java-archive\omni-mtg.jar" "%d%target\temp\omni-mtg\omni-mtg-java-archive\omni-mtg.jar"


Powershell.exe -executionpolicy remotesigned -File zip.ps1

echo ready
echo %d%target\universal\omni-mtg.zip