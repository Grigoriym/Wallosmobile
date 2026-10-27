package com.grappim.wallosmobile.core.storage.customheaders

/** Why [CustomHeaderValidator] won't send a row (plan §4.5b). */
enum class CustomHeaderError {
    MissingName,
    InvalidName,

    /** A header the HTTP stack or the web login sets itself — sending a second one breaks them. */
    ReservedName,
    DuplicateName,
    InvalidValue
}
