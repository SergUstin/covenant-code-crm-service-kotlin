package com.covenantcode.crm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CrmServiceApplication

fun main(args: Array<String>) {
    runApplication<CrmServiceApplication>(*args)
}
