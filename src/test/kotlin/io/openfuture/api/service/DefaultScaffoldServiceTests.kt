package io.openfuture.api.service

import io.openfuture.api.component.scaffold.ScaffoldCompiler
import io.openfuture.api.component.web3.Web3Wrapper
import io.openfuture.api.config.UnitTest
import io.openfuture.api.config.any
import io.openfuture.api.config.propety.EthereumProperties
import io.openfuture.api.domain.scaffold.*
import io.openfuture.api.entity.auth.OpenKey
import io.openfuture.api.entity.auth.User
import io.openfuture.api.entity.scaffold.PropertyType.STRING
import io.openfuture.api.entity.scaffold.Scaffold
import io.openfuture.api.entity.scaffold.ScaffoldSummary
import io.openfuture.api.exception.NotFoundException
import io.openfuture.api.repository.ScaffoldPropertyRepository
import io.openfuture.api.repository.ScaffoldRepository
import io.openfuture.api.repository.ScaffoldSummaryRepository
import io.openfuture.api.repository.ShareHolderRepository
import org.assertj.core.api.Assertions.assertThat
import org.ethereum.solidity.compiler.CompilationResult
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.web3j.crypto.Credentials
import java.math.BigInteger
import java.util.*

internal class DefaultScaffoldServiceTests : UnitTest() {

    @Mock private lateinit var compiler: ScaffoldCompiler
    @Mock private lateinit var repository: ScaffoldRepository
    @Mock private lateinit var properties: EthereumProperties
    @Mock private lateinit var openKeyService: OpenKeyService
    @Mock private lateinit var propertyRepository: ScaffoldPropertyRepository
    @Mock private lateinit var scaffoldSummaryRepository: ScaffoldSummaryRepository
    @Mock private lateinit var shareHolderRepository: ShareHolderRepository

    @Mock private lateinit var web3: Web3Wrapper
    @Mock private lateinit var pageable: Pageable
    @Mock private lateinit var credentials: Credentials

    private lateinit var service: ScaffoldService


    @Before
    fun setUp() {
        service = DefaultScaffoldService(web3, repository, propertyRepository, scaffoldSummaryRepository,
                shareHolderRepository ,compiler, properties, openKeyService)
    }

    @Test
    fun getAllTest() {
        val user = createUser()
        val expectedScaffoldPages = PageImpl(Collections.singletonList(createScaffold()), pageable, 1)

        given(repository.findAllByOpenKeyUserOrderByIdDesc(user, pageable)).willReturn(expectedScaffoldPages)

        val actualScaffoldPages = service.getAll(user, pageable)

        assertThat(actualScaffoldPages).isEqualTo(expectedScaffoldPages)
    }

    @Test
    fun getTest() {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()
        val expectedScaffold = createScaffold()

        given(repository.findByAddressAndOpenKeyUser(addressValue, user)).willReturn(expectedScaffold)

        val actualScaffold = service.get(addressValue, user)

        assertThat(actualScaffold).isEqualTo(expectedScaffold)
    }

    @Test(expected = NotFoundException::class)
    fun getWhenScaffoldNotFoundShouldTrowExceptionTest() {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()

        given(repository.findByAddressAndOpenKeyUser(addressValue, user)).willReturn(null)

        service.get(addressValue, user)
    }

    @Test
    fun compileTest() {
        val openKeyValue = "op_pk_9de7cbb4-857c-49e9-87d2-fc91428c4c12"
        val user = createUser()
        val openKey = OpenKey(user, Date(), openKeyValue)
        val request = CompileScaffoldRequest(openKeyValue)
        val contractMetadata = CompilationResult.ContractMetadata().apply { abi = "abi"; bin = "bin" }
        val expectedContractMetadata = CompiledScaffoldDto(contractMetadata)

        given(openKeyService.get(openKeyValue)).willReturn(openKey)
        given(scaffoldSummaryRepository.countByEnabledIsFalseAndScaffoldOpenKeyUser(user)).willReturn(1)
        given(compiler.compile(request.properties)).willReturn(contractMetadata)
        given(properties.allowedDisabledContracts).willReturn(10)

        val actualContractMetadata = service.compile(request)

        assertThat(actualContractMetadata).isEqualTo(expectedContractMetadata)
    }

    @Test(expected = IllegalStateException::class)
    fun compileWhenExceedingNumberOfAvailableDisabledScaffoldsShouldThrowExceptionTest() {
        val openKeyValue = "op_pk_9de7cbb4-857c-49e9-87d2-fc91428c4c12"
        val user = createUser()
        val openKey = OpenKey(user, Date(), openKeyValue)
        val request = CompileScaffoldRequest(openKeyValue)

        given(openKeyService.get(openKeyValue)).willReturn(openKey)
        given(scaffoldSummaryRepository.countByEnabledIsFalseAndScaffoldOpenKeyUser(user)).willReturn(11)
        given(properties.allowedDisabledContracts).willReturn(10)

        service.compile(request)

        verify(compiler, never()).compile(request.properties)
    }

    @Test
    fun updateTest() {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()
        val description = "description"
        val scaffold = createScaffold()
        val request = UpdateScaffoldRequest(description)

        given(repository.findByAddressAndOpenKeyUser(addressValue, user)).willReturn(scaffold)
        given(repository.save(any(Scaffold::class.java))).will { invocation -> invocation.arguments[0] }

        val actualScaffold = service.update(addressValue, user, request)

        assertThat(actualScaffold.address).isEqualTo(scaffold.address)
        assertThat(actualScaffold.description).isEqualTo(description)
    }

    @Test
    fun setWebHookTest() {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()
        val webHookValue = "webHook"
        val expectedScaffold = createScaffold()
        expectedScaffold.webHook = webHookValue
        val request = SetWebHookRequest(webHookValue)

        given(repository.findByAddressAndOpenKeyUser(addressValue, user)).willReturn(expectedScaffold)
        given(repository.save(expectedScaffold)).will { invocation -> invocation.arguments[0] }

        val actualScaffold = service.setWebHook(addressValue, request, user)

        assertThat(actualScaffold).isEqualTo(expectedScaffold)
    }

    @Test
    fun getScaffoldSummaryWhenExpiredCachePeriodShouldReturnCachedScaffoldSummaryTest() {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()
        val scaffold = createScaffold()
        val scaffoldSummary = ScaffoldSummary(scaffold, BigInteger.ONE, BigInteger.ONE, false)

        given(repository.findByAddressAndOpenKeyUser(addressValue, user)).willReturn(scaffold)
        given(scaffoldSummaryRepository.findByScaffold(scaffold)).willReturn(scaffoldSummary)
        given(properties.cachePeriodInMinutest).willReturn(10)

        val actualScaffoldSummary = service.getScaffoldSummary(addressValue, user)

        assertThat(actualScaffoldSummary).isEqualTo(scaffoldSummary)
    }

    @Test
    fun getQuotaTest() {
        val user = createUser()
        val currentCount = 1
        val expectedQuota = ScaffoldQuotaDto(currentCount, 10)

        given(scaffoldSummaryRepository.countByEnabledIsFalseAndScaffoldOpenKeyUser(user)).willReturn(currentCount)
        given(properties.enabledContactTokenCount).willReturn(10)

        val actualQuota = service.getQuota(user)

        assertThat(actualQuota).isEqualTo(expectedQuota)
    }

    private fun createScaffold(): Scaffold {
        val addressValue = "0xba37163625b3f2e96112562858c12b75963af138"
        val user = createUser()
        val openKey = OpenKey(user.apply { id = 1L }, null, "op_pk_9de7cbb4-857c-49e9-87d2-fc91428c4c12").apply { id = 1L }

        return Scaffold(addressValue, openKey, "abi", addressValue, "description", "1", 1,
                "1", "webHook", mutableListOf())
    }

    private fun createScaffoldPropertyDto(): ScaffoldPropertyDto = ScaffoldPropertyDto("name", STRING, "value")

    private fun createUser(): User = User("104113085667282103363")

}