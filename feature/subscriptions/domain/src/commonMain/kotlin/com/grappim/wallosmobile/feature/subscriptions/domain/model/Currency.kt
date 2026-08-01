package com.grappim.wallosmobile.feature.subscriptions.domain.model

/**
 * A currency the instance knows about. v1 reads it for one reason — a price is unrenderable
 * without the [symbol] its `currency_id` points at (plan §7.1).
 *
 * `rate` and `in_use` are on the wire but not here: conversion is Phase 2b and the in-use flag
 * only guards deletes, which is Phase 5.
 */
data class Currency(val id: Int, val name: String, val symbol: String, val code: String)
