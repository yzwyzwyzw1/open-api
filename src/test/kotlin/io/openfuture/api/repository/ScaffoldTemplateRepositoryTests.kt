package io.openfuture.api.repository

import io.openfuture.api.config.RepositoryTests
import io.openfuture.api.entity.scaffold.ScaffoldTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ScaffoldTemplateRepositoryTests : RepositoryTests() {

    @Autowired private lateinit var repository: ScaffoldTemplateRepository


    @Test
    fun findAllByUserAndDeletedIsFalse() {
        val scaffoldTemplate = ScaffoldTemplate("template", "developerAddress", "description", "fiat_amount",
                1, "conversionAmount", "webHook")
        val deletedScaffoldTemplate = ScaffoldTemplate("deleted_template", "developerAddress", "description", "fiat_amount",
                1, "conversionAmount", "webHook", mutableListOf(), true)
        entityManager.persist(scaffoldTemplate)
        entityManager.persist(deletedScaffoldTemplate)

        val actualScaffoldTemplate = repository.findAllByDeletedIsFalse()

        assertThat(actualScaffoldTemplate).isEqualTo(listOf(scaffoldTemplate))
    }

}
