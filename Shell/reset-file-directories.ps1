foreach ($directory in 'Server', 'Client')
{
    $path = "..\$directory\src\main\resources\FilesDirectory\"
    if(Test-Path $path)
    {
        Get-Childitem "..\$directory\src\main\resources\FilesDirectory\" -File | Foreach-Object {Remove-Item $_.FullName}
    }
    else
    {
        mkdir $path
    }
}