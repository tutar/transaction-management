class TransactionApp {
    constructor() {
        this.apiBaseUrl = '/api/transactions';
        this.form = document.getElementById('transactionForm');
        this.tableBody = document.querySelector('#transactionTable tbody');
        this.init();
    }

    init() {
        this.currentPage = 1;
        this.pageSize = 10;
        this.totalTransactions = 0;
        
        this.loadTransactions();
        this.setupForm();
        this.setupAddButton();
        this.setupPagination();
    }

    async loadTransactions() {
        try {
            const url = `${this.apiBaseUrl}?page=${this.currentPage}&size=${this.pageSize}`;
            const response = await fetch(url);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const page = await response.json();
            console.log('API Response:', page);  // 调试用
            
            // 确保数据格式正确
            if (!page || !page.content || !Array.isArray(page.content)) {
                throw new Error('Invalid data format');
            }
            
            this.totalTransactions = page.totalElements;
            this.renderTable(page.content);
            this.updatePagination(page.totalPages);
        } catch (error) {
            console.error('加载交易失败:', error);
            alert('无法加载交易数据: ' + error.message);
        }
    }

    setupAddButton() {
        const addBtn = document.getElementById('addTransactionBtn');
        const formContainer = document.querySelector('.transaction-form');
        
        const overlay = document.getElementById('overlay');
        
        addBtn.addEventListener('click', () => {
            document.body.classList.add('modal-open');
            overlay.style.display = 'block';
            formContainer.style.display = 'block';
            this.resetForm();
        });

        const closeModal = () => {
            document.body.classList.remove('modal-open');
            overlay.style.display = 'none';
            formContainer.style.display = 'none';
            this.resetForm();
        };

        document.getElementById('cancelBtn').addEventListener('click', closeModal);
        overlay.addEventListener('click', closeModal);
    }

    setupPagination() {
        const pagination = document.createElement('div');
        pagination.className = 'pagination';
        pagination.innerHTML = `
            <button id="prevPage" disabled>上一页</button>
            <span id="pageInfo"></span>
            <button id="nextPage" disabled>下一页</button>
        `;
        document.querySelector('.transaction-list').appendChild(pagination);

        document.getElementById('prevPage').addEventListener('click', () => {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.loadTransactions();
            }
        });

        document.getElementById('nextPage').addEventListener('click', () => {
            if (this.currentPage < this.totalPages) {
                this.currentPage++;
                this.loadTransactions();
            }
        });
    }

    updatePagination(totalPages) {
        this.totalPages = totalPages;
        const pageInfo = document.getElementById('pageInfo');
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');

        pageInfo.textContent = `第 ${this.currentPage} 页，共 ${totalPages} 页`;
        prevBtn.disabled = this.currentPage === 1;
        nextBtn.disabled = this.currentPage === totalPages;
    }

    renderTable(transactions) {
        if (!transactions || transactions.length === 0) {
            this.tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="empty-message">暂无交易记录</td>
                </tr>
            `;
            return;
        }
        
        this.tableBody.innerHTML = transactions.map(transaction => `
            <tr>
                <td>${transaction.id}</td>
                <td>${this.translateType(transaction.type)}</td>
                <td>${transaction.amount.toFixed(2)}</td>
                <td>${this.translateStatus(transaction.status)}</td>
                <td class="action-buttons">
                    <button onclick="app.editTransaction(${transaction.id})">编辑</button>
                    <button onclick="app.deleteTransaction(${transaction.id})">删除</button>
                </td>
            </tr>
        `).join('');
    }

    translateType(type) {
        const typeMap = {
            'FEE_INCOME': '费用收入',
            'INTEREST_INCOME': '利息收入',
            'DEPOSIT': '存款',
            'REFUND': '退款',
            'FEE_EXPENSE': '费用支出',
            'INTEREST_EXPENSE': '利息支出',
            'WITHDRAWAL': '取款',
            'TRANSFER': '转账'
        };
        return typeMap[type] || type;
    }

    translateStatus(status) {
        switch (status) {
            case 'PENDING': return '待处理';
            case 'COMPLETED': return '已完成';
            case 'CANCELLED': return '已取消';
            default: return status;
        }
    }

    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = this.getFormData();
            
            try {
                if (formData.id) {
                    await this.updateTransaction(formData);
                } else {
                    await this.createTransaction(formData);
                }
                this.resetForm();
                await this.loadTransactions();
            } catch (error) {
                console.error('操作失败:', error);
                alert('操作失败，请重试');
            }
        });

        document.getElementById('cancelBtn')?.addEventListener('click', () => {
            this.resetForm();
        });
    }

    getFormData() {
        return {
            id: parseInt(document.getElementById('transactionId').value) || 0,
            type: document.getElementById('type').value,
            amount: parseFloat(document.getElementById('amount').value),
            status: document.getElementById('status').value
        };
    }

    async createTransaction(transaction) {
        const response = await fetch(this.apiBaseUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(transaction)
        });
        if (!response.ok) throw new Error('创建失败');
    }

    async updateTransaction(transaction) {
        const response = await fetch(`${this.apiBaseUrl}/${transaction.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(transaction)
        });
        if (!response.ok) throw new Error('更新失败');
    }

    async deleteTransaction(id) {
        if (!confirm('确定要删除此交易吗？')) return;
        
        const response = await fetch(`${this.apiBaseUrl}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('删除失败');
        await this.loadTransactions();
    }

    resetForm() {
        this.form.reset();
        document.getElementById('formTitle').textContent = '添加交易';
        document.getElementById('transactionId').value = '';
    }

    async editTransaction(id) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/${id}`);
            const transaction = await response.json();
            
            document.getElementById('transactionId').value = transaction.id;
            document.getElementById('type').value = transaction.type;
            document.getElementById('amount').value = transaction.amount;
            document.getElementById('status').value = transaction.status;
            document.getElementById('formTitle').textContent = '编辑交易';
            
            // 显示编辑表单
            document.body.classList.add('modal-open');
            document.getElementById('overlay').style.display = 'block';
            document.querySelector('.transaction-form').style.display = 'block';
        } catch (error) {
            console.error('加载交易失败:', error);
            alert('无法加载交易数据');
        }
    }
}

const app = new TransactionApp();
