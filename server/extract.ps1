Add-Type -A System.IO.Compression.FileSystem

[IO.Compression.ZipFile]::ExtractToDirectory("target/universal/omnimtg-0.1-SNAPSHOT.zip", "target/temp")





