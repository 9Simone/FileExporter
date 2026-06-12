package com.exporter.cli

import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CommandLine(ExportCommand()).execute(*args)
    exitProcess(exitCode)
}
