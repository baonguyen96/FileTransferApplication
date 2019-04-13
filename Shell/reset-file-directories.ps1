foreach ($directory in 'Server', 'Client') {
    Remove-Item "..\$directory\src\main\resources\FilesDirectory\*.*"
}