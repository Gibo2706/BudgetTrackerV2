package site.giboworks.budgettracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.giboworks.budgettracker.data.local.dao.FixedBillDao
import site.giboworks.budgettracker.data.local.entity.FixedBillEntity
import site.giboworks.budgettracker.domain.model.BillsSummary
import site.giboworks.budgettracker.domain.model.FixedBill
import site.giboworks.budgettracker.domain.repository.FixedBillRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FixedBillRepository.
 * 
 * Handles data access for fixed monthly bills using Room database.
 */
@Singleton
class FixedBillRepositoryImpl @Inject constructor(
    private val fixedBillDao: FixedBillDao
) : FixedBillRepository {
    
    // ==================== OBSERVE ====================
    
    override fun observeAll(): Flow<List<FixedBill>> {
        return fixedBillDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeUnpaid(): Flow<List<FixedBill>> {
        return fixedBillDao.observeUnpaid().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observePaid(): Flow<List<FixedBill>> {
        return fixedBillDao.observePaid().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun observeById(id: Long): Flow<FixedBill?> {
        return fixedBillDao.observeById(id).map { entity ->
            entity?.toDomain()
        }
    }
    
    override fun observeTotalCount(): Flow<Int> {
        return fixedBillDao.observeTotalCount()
    }
    
    override fun observePaidCount(): Flow<Int> {
        return fixedBillDao.observePaidCount()
    }
    
    override fun observeTotalEstimated(): Flow<Double> {
        return fixedBillDao.observeTotalEstimated()
    }
    
    override fun observeTotalActualPaid(): Flow<Double> {
        return fixedBillDao.observeTotalActualPaid()
    }
    
    override fun observeSavingsFromBills(): Flow<Double> {
        return fixedBillDao.observeSavingsFromBills()
    }
    
    override fun observeOverageFromBills(): Flow<Double> {
        return fixedBillDao.observeOverageFromBills()
    }
    
    // ==================== GET ====================
    
    override suspend fun getAll(): List<FixedBill> {
        return fixedBillDao.getAll().map { it.toDomain() }
    }
    
    override suspend fun getById(id: Long): FixedBill? {
        return fixedBillDao.getById(id)?.toDomain()
    }
    
    override suspend fun getTotalEstimated(): Double {
        return fixedBillDao.getTotalEstimated()
    }
    
    override suspend fun getBillsSummary(): BillsSummary {
        val bills = getAll()
        return BillsSummary.fromBills(bills)
    }
    
    // ==================== MUTATE ====================
    
    override suspend fun insert(bill: FixedBill): Long {
        return fixedBillDao.insert(FixedBillEntity.fromDomain(bill))
    }
    
    override suspend fun insertAll(bills: List<FixedBill>) {
        fixedBillDao.insertAll(bills.map { FixedBillEntity.fromDomain(it) })
    }
    
    override suspend fun update(bill: FixedBill) {
        fixedBillDao.update(FixedBillEntity.fromDomain(bill))
    }
    
    override suspend fun delete(bill: FixedBill) {
        fixedBillDao.delete(FixedBillEntity.fromDomain(bill))
    }
    
    override suspend fun deleteById(id: Long) {
        fixedBillDao.deleteById(id)
    }
    
    override suspend fun deleteAll() {
        fixedBillDao.deleteAll()
    }
    
    // ==================== PAYMENT OPERATIONS ====================
    
    override suspend fun markAsPaid(billId: Long, actualAmount: Double?) {
        fixedBillDao.markAsPaid(billId, actualAmount)
    }
    
    override suspend fun markAsUnpaid(billId: Long) {
        fixedBillDao.markAsUnpaid(billId)
    }
    
    override suspend fun resetAllBillsForNewCycle() {
        fixedBillDao.resetAllBillsForNewCycle()
    }
}
