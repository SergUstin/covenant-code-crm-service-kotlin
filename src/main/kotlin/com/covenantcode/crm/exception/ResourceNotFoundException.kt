package com.covenantcode.crm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(resource: String, id: Long) : super("$resource not found with id: $id")
}
