package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void transferMoney_success() throws Exception {
    String uniqueAccountIdFrom = "Id-1";
    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("123.45"));
    String uniqueAccountIdTo = "Id-2";
    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("0"));
    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);
    this.mockMvc.perform(put("/v1/accounts/" + uniqueAccountIdFrom+"/"+uniqueAccountIdTo+"/"+3.45))
            .andExpect(status().isOk());
  }

  @Test
  void transferMoney_accountFromNotExists() throws Exception {
    String uniqueAccountIdFrom = "Id-3";
    String uniqueAccountIdTo = "Id-4";
    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("0"));
    this.accountsService.createAccount(accountTo);
    MvcResult result = this.mockMvc.perform(put("/v1/accounts/" + uniqueAccountIdFrom+"/"+uniqueAccountIdTo+"/"+3.45))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("Account from with id "+uniqueAccountIdFrom+" not found!" , result.getResponse().getContentAsString());
  }

  @Test
  void transferMoney_accountToNotExists() throws Exception {
    String uniqueAccountIdFrom = "Id-5";
    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("123.45"));
    String uniqueAccountIdTo = "Id-6";
    this.accountsService.createAccount(accountFrom);
    MvcResult result = this.mockMvc.perform(put("/v1/accounts/" + uniqueAccountIdFrom+"/"+uniqueAccountIdTo+"/"+3.45))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("Account to with id "+uniqueAccountIdTo+" not found!" , result.getResponse().getContentAsString());
  }

  @Test
  void transferMoney_notEnoughMoney() throws Exception {
    String uniqueAccountIdFrom = "Id-7";
    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("123.45"));
    String uniqueAccountIdTo = "Id-8";
    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("0"));
    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);
    MvcResult result = this.mockMvc.perform(put("/v1/accounts/" + uniqueAccountIdFrom+"/"+uniqueAccountIdTo+"/"+123.46))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("The amount "+123.46+" is lower than the current balance in the from account which has "+accountFrom.getBalance() , result.getResponse().getContentAsString());
  }
}
