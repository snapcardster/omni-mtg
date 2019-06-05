Add-Type -A System.IO.Compression.FileSystem

[IO.Compression.ZipFile]::CreateFromDirectory("target/temp", "target/universal/omni-mtg.zip")