fun main(args: Array<String>) {
    val exitCode = CommandLine(ExportCommand()).execute(*args)
    exitProcess(exitCode)
}