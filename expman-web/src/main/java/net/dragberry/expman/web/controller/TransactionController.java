package net.dragberry.expman.web.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import net.dragberry.expman.bean.AccountTO;
import net.dragberry.expman.bean.CounterPartyTO;
import net.dragberry.expman.bean.ResultListTO;
import net.dragberry.expman.bean.TransactionTO;
import net.dragberry.expman.bean.TransactionTypeTO;
import net.dragberry.expman.bean.ResultTO;
import net.dragberry.expman.business.AccountService;
import net.dragberry.expman.business.CounterPartyService;
import net.dragberry.expman.business.ReferenceService;
import net.dragberry.expman.business.TransactionService;
import net.dragberry.expman.business.TransactionService.Type;
import net.dragberry.expman.business.TransactionTypeService;
import net.dragberry.expman.messages.BusinessMessageCodes;
import net.dragberry.expman.query.AccountQuery;
import net.dragberry.expman.query.TransactionCreateQuery;
import net.dragberry.expman.query.TransactionListQuery;
import net.dragberry.expman.query.DeleteTransactionQuery;
import net.dragberry.expman.web.common.Constants;
import net.dragberry.expman.web.controller.error.IssueResolver;
import net.dragberry.expman.web.security.ExpmanSecurityContext;
import net.dragberry.expman.web.session.TransactionListSession;

@Controller
public class TransactionController implements Serializable {

	private static final String CURRENCY_LIST = "currencyList";

	private static final String ACCOUNT_LIST = "accountList";

	private static final String TRANSACTION = "transaction";

	private static final String COUNTER_PARTY_LIST = "counterPartyList";

	private static final String TRANSACTION_TYPE_LIST = "transactionTypeList";

	private static final long serialVersionUID = 4920765963061089750L;

	private static final Logger LOG = LogManager.getLogger(TransactionController.class.getName());
	
	private static final String TRANSACTION_LIST_SESSION = "transactionListSession";
	
	private static final String TYPE_LIST = "typeList";
	
	private static final Map<String, String> errorMap = new HashMap<>();

	static {
		errorMap.put(BusinessMessageCodes.CreateTransaction.AMOUNT_IS_INCORRECT, TransactionCreateQuery.AMOUNT_FIELD);
		errorMap.put(BusinessMessageCodes.CreateTransaction.AMOUNT_IS_MANDATORY, TransactionCreateQuery.AMOUNT_FIELD);
		errorMap.put(BusinessMessageCodes.CreateTransaction.DESCRIPTION_IS_MANDATORY, TransactionCreateQuery.DESCRIPTION_FIELD);
		errorMap.put(BusinessMessageCodes.CreateTransaction.DESCRIPTION_IS_TOO_LARGE, TransactionCreateQuery.DESCRIPTION_FIELD);
		errorMap.put(BusinessMessageCodes.CreateTransaction.CURRENCY_IS_NOT_MATCH_WITH_ACCOUNT_CURRENCY, TransactionCreateQuery.CURRENCY_FIELD);
	}

	@Autowired
	private TransactionService transactionService;
	@Autowired
	private TransactionTypeService transactionTypeService;
	@Autowired
	private CounterPartyService counterPartyService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ReferenceService referenceService;
	
	@Autowired
	private TransactionListSession transactionListSession;
	
	@RequestMapping(value = Constants.Path.TRANSACTION_DELETE)
	public ModelAndView deleteTransaction(@PathVariable("transactionId") Long transactionId) {
		DeleteTransactionQuery query = new DeleteTransactionQuery();
		query.setTransactionKey(transactionId);
		query.setCustomerKey(ExpmanSecurityContext.getCustomerKey());
		transactionService.deleteTransaction(query);
		return new ModelAndView("redirect:/transaction/list");
	}
	
	@RequestMapping(value = Constants.Path.TRANSACTION_LIST)
	public ModelAndView listTransaction() {
		TransactionListQuery query = new TransactionListQuery();
		query.setCustomerKey(ExpmanSecurityContext.getCustomerKey());
		ResultListTO<TransactionTO> result = transactionService.fetchTransactions(query);
		transactionListSession.setTransactionList(result.getList());
		transactionListSession.getPaginator().setCurrentPage(1);
		transactionListSession.getPaginator().setPageSize(result.getPageSize());
		transactionListSession.getPaginator().setTotalPages(26);
		transactionListSession.setInitialized(true);
		ModelAndView modelAndView = new ModelAndView(Constants.View.TRANSACTION_LIST);
		modelAndView.addObject(TRANSACTION_LIST_SESSION, transactionListSession);
		return modelAndView;
	}
	
	@RequestMapping(value = Constants.Path.TRANSACTION_CREATE)
	public ModelAndView createTransaction() {
		ModelAndView modelAndView = prepareCreateTransactionScreen(new TransactionCreateQuery());
		return modelAndView;
	}

	@RequestMapping(value = Constants.Path.TRANSACTION_CREATE, method = RequestMethod.POST)
	public ModelAndView createTransaction(@ModelAttribute(TRANSACTION)@Valid TransactionCreateQuery transaction, BindingResult bindingResult, HttpServletRequest request) {
		transaction.setCustomerKey(ExpmanSecurityContext.getCustomerKey());
		ResultTO<TransactionTO> result = transactionService.createTransaction(transaction);
		if (result.hasIssues()) {
			ModelAndView modelAndView = prepareCreateTransactionScreen(transaction);
			Map<String, Object> errorModel = IssueResolver.resolve(result.getIssueLog(), errorMap, bindingResult);
			modelAndView.addAllObjects(errorModel);
			return modelAndView;
		} else {
			return new ModelAndView(Constants.View.HOME);
		}
	}
	
	private ModelAndView prepareCreateTransactionScreen(TransactionCreateQuery transaction) {
		ModelAndView modelAndView = new ModelAndView(Constants.View.TRANSACTION_CREATE);
		modelAndView.addObject(TRANSACTION, transaction);
		Long loggedCutomerKey = ExpmanSecurityContext.getCustomerKey();
		
		List<TransactionTypeTO> transactionTypeList = transactionTypeService.findTransactionTypeListForCustomer(loggedCutomerKey).getList();
		modelAndView.addObject(TRANSACTION_TYPE_LIST, transactionTypeList);
		
		List<CounterPartyTO> counterPartyList = counterPartyService.fetchCounterPartyList(loggedCutomerKey).getList();
		modelAndView.addObject(COUNTER_PARTY_LIST, counterPartyList);
		
		List<String> currencyList = referenceService.fecthCurrecnyList();
		modelAndView.addObject(CURRENCY_LIST, currencyList);
		
		Type[] typeList = Type.values();
		modelAndView.addObject(TYPE_LIST, typeList);
		
		AccountQuery accountQuery = new AccountQuery();
		accountQuery.setCustomerKey(loggedCutomerKey);
		List<AccountTO> accountList = accountService.fetchAccounts(accountQuery).getList();
		modelAndView.addObject(ACCOUNT_LIST, accountList);
		
		return modelAndView;
	}
}
