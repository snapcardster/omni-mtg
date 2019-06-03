Add-Type -A System.IO.Compression.FileSystem

[IO.Compression.ZipFile]::CreateFromDirectory("target/temp/omnimtg-0.1-SNAPSHOT", "target/universal/omni-mtg.zip")