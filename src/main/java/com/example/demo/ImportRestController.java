package com.example.demo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.CollaboratorApi;
import io.swagger.client.api.ContractApi;
import io.swagger.client.api.UserApi;
import io.swagger.client.auth.OAuth;
import io.swagger.client.model.CollaboratorFieldsRequestDTO;
import io.swagger.client.model.CollaboratorFieldsRequestDTO.RoleEnum;
import io.swagger.client.model.CollaboratorFieldsRequestDTO.SideEnum;
import io.swagger.client.model.CompanyFieldsDTO;
import io.swagger.client.model.ContractCollaboratorResponseDTO;
import io.swagger.client.model.ContractFieldRequestDTO;
import io.swagger.client.model.CreateCollaboratorIdentityFieldV1DTO;
import io.swagger.client.model.CreateContractRequestDTO;
import io.swagger.client.model.CreateUserRequestDTO;
import io.swagger.client.model.Link;
import io.swagger.client.model.UpdateFieldsCollaboratorDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/import")
@Slf4j
public class ImportRestController {

    private DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");
    private DateTimeFormatter dtfOut = DateTimeFormat.forPattern("EEEE, dd MMMM yyyy");
    
    
    private long OWNER_USER_ID = 6749541852251968L;
    private long TEAM_ID = 6753931778590669L;

    private long CONTRACT_TEMPLATE_ID = 225L;
    private long CONTRACT_TYPE_ID = 6690589634461743L;

    private CollaboratorApi collaboratorApiClient = new CollaboratorApi();

    @GetMapping
    public void importContracts(@RequestParam("csv") MultipartFile csv) throws IOException, CsvException {
        ApiClient defaultClient = Configuration.getDefaultApiClient();

        // Configure OAuth2 access token for authorization: auth0_jwt
        OAuth auth0_jwt = (OAuth) defaultClient.getAuthentication("auth0_jwt");
        auth0_jwt.setAccessToken(
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InoyV3QwbUNMYWsxWF95Uk1PRVYxQiJ9.eyJodHRwczovL2FwcC5sZWdpc2xhdGUudGVjaC91c2VyX2lkIjoiNjc0OTU0MTg1MjI1MTk2OCIsImlzcyI6Imh0dHBzOi8vbG9naW4ubGVnaXNsYXRlLnRlY2gvIiwic3ViIjoidFN2cDFlR0pQTUxBMUdzYmRldnVMSEd4ckF2VzJSWmdAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vYXBpLmxlZ2lzbGF0ZS50ZWNoIiwiaWF0IjoxNjU3MjkwMTcwLCJleHAiOjE2NTczNzY1NzAsImF6cCI6InRTdnAxZUdKUE1MQTFHc2JkZXZ1TEhHeHJBdlcyUlpnIiwic2NvcGUiOiJjcmVhdGU6Y29udHJhY3RzIHJlYWQ6Y29udHJhY3RzIGRlbGV0ZTpjb250cmFjdHMiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.ody4qf4pJQSUCUtofe3JKSd5BStS95rIQqJvC9DGvzBAOz1djVUEKXY6eIjBnxdfOVgf9H9BQM_OjwYg-Tc2yo0YMTFw7VT0nRyqAI8TDxUp6GrmN4IWATz7c4PToDWtvzjX-G1ZJtFSO1SegRJJ8cuSO83Un2BKiDcAZ_n5fqud3XcHJUINHDX-p56NwfBOMwAoVCroqbwMhKUOxzoGbJgQnEDq_qqZ2TCCnj6HhVB2byte8OKHtseKaWXizBKADrp4PPmUcKQqcaCJWDZwbLpkL77JfS1LHCWoEFNYmSQtcP6Dogrs0uVETpj1HcMLdnm2dk-rHg8IjVY0kp2XIw");

        FileUtils.writeByteArrayToFile(new File("file.csv"), csv.getBytes());

        try (CSVReader reader = new CSVReader(new FileReader("file.csv"))) {
            List<String[]> lines = reader.readAll();
            lines.forEach(line -> {

                try {
                    long userId = 0;
                    userId = createUser(line);

                    long contractId = 0;
                    contractId = createContract(line);

                    inviteSecondSideParty(line, userId, contractId);

                    ContractCollaboratorResponseDTO firstSideCollaborator = null;

                    firstSideCollaborator = firstSideCollaborator(contractId);

                    log.info("Chaning role to signatory " + firstSideCollaborator.getId());
                    updateRoleToSignatory(firstSideCollaborator.getId());

                    log.info("Adding Trinity College company to the first side " + firstSideCollaborator.getId());
                    inviteFirstSideCompany(contractId, firstSideCollaborator.getId());

                    log.info("Signing the contract on behalf of Trinity College");
                    sign(firstSideCollaborator.getId());
                    
                    log.info("Contract " + contractId + " imported successfully");
                } catch (ApiException e) {
                    log.info(e.getResponseBody());
                }
            });
        }
    }

    private void sign(Long collaboratorId) throws ApiException {
        
        // FIXME
        //collaboratorApiClient.updateCollaboratorSignatureUsingPATCH(collaboratorId);
    }

    private ContractCollaboratorResponseDTO firstSideCollaborator(long contractId) throws ApiException {

        List<ContractCollaboratorResponseDTO> results = collaboratorApiClient.getCollaboratorsUsingGET(contractId);

        ContractCollaboratorResponseDTO collaborator = results.stream()
                .filter(result -> result.getUserId() == OWNER_USER_ID).findFirst().get();

        return collaborator;
    }

    private void updateRoleToSignatory(long collaboratorId) throws ApiException {

        UpdateFieldsCollaboratorDTO body = new UpdateFieldsCollaboratorDTO(); // UpdateFieldsCollaboratorDTO | fields

        body.setRoles(Arrays.asList(io.swagger.client.model.UpdateFieldsCollaboratorDTO.RolesEnum.COLLABORATOR,
                io.swagger.client.model.UpdateFieldsCollaboratorDTO.RolesEnum.SIGNATORY));

        ContractCollaboratorResponseDTO result = collaboratorApiClient.updateCollaboratorRolesUsingPATCH(body,
                collaboratorId);

    }

    private void inviteFirstSideCompany(long contractId, long signatoryId) throws ApiException {
        CollaboratorApi apiInstance = new CollaboratorApi();
        CompanyFieldsDTO body = new CompanyFieldsDTO(); // CompanyFieldsDTO | companyFieldsDTORequest

        body.setId(249L);
        body.setCreatedBy(OWNER_USER_ID);
        body.setContractConfigurationId(contractId);
        body.setRole(io.swagger.client.model.CompanyFieldsDTO.RoleEnum.PARTY);
        body.setSide(io.swagger.client.model.CompanyFieldsDTO.SideEnum.FIRST);
        body.setSignatoryRelatedCompany(signatoryId);

        apiInstance.postCompanyUsingPOST(body, contractId);

    }

    private void inviteSecondSideParty(String[] line, long userId, long contractId) throws ApiException {

        String studentFirstName = line[5]; // 2776
        String studentLastName = line[6]; // 2778
        String email = line[10];

        CollaboratorApi apiInstance = new CollaboratorApi();
        CollaboratorFieldsRequestDTO body = new CollaboratorFieldsRequestDTO(); // CollaboratorFieldsRequestDTO |
                                                                                // createCollaboratorRequestDTO

        CreateCollaboratorIdentityFieldV1DTO firstName = new CreateCollaboratorIdentityFieldV1DTO();
        firstName.setId(2776L);
        firstName.setValue(studentFirstName);

        CreateCollaboratorIdentityFieldV1DTO secondName = new CreateCollaboratorIdentityFieldV1DTO();
        secondName.setId(2778L);
        secondName.setValue(studentLastName);

        CreateCollaboratorIdentityFieldV1DTO emailField = new CreateCollaboratorIdentityFieldV1DTO();
        emailField.setId(2779L);
        emailField.setValue(email);

        List<CreateCollaboratorIdentityFieldV1DTO> identityFields = Arrays.asList(firstName, secondName, emailField);

        body.setUserId(userId);
        body.setInviterId(OWNER_USER_ID);
        body.setSide(SideEnum.SECOND);
        body.setRole(Arrays.asList(RoleEnum.PARTY));
        body.setIdentityFields(identityFields);

        Link result = apiInstance.postCollaboratorUsingPOST(body, contractId);
        log.info(result.getHref());

    }

    @Data
    class Contract {
        private Field contractName = new Field(6069L, "contract_name");
        private Field area = new Field(6050, "contract_location");
        private Field room = new Field(6051L, "contract_roomDetails");
        private Field roomType = new Field(6052L, "contract_roomType");
        private Field bedSize = new Field(6072L, "contract_bedSize");

        private Field licenseStartDate = new Field(6053L, "contract_startDate");
        private Field licenseEndDate = new Field(6054L, "contract_endDate");

        private Field totalAccommodationFee = new Field(6055L, "contract_licenceFee");

        private Field mtPaymentDate = new Field(6056L, "contract_firstPaymentDate");
        private Field mtPaymentAmount = new Field(6059L, "contract_firstPaymentAmount");

        private Field htPaymentDate = new Field(6057L, "contract_secondPaymentDate");
        private Field htPaymentAmount = new Field(6060, "contract_secondPaymentAmount");

        private Field ttPaymentDate = new Field(6058, "contract_thirdPaymentDate");
        private Field ttPaymentAmount = new Field(6061, "contract_thirdPaymentAmount");

        private Field mtStart = new Field(6062L, "contract_firstOccupationStart");
        private Field mtEnd = new Field(6063, "contract_firstOccupationEnd");

        private Field htStart = new Field(6064, "contract_secondOccupationStart");
        private Field htEnd = new Field(6065, "contract_secondOccupationEnd");

        private Field ttStart = new Field(6066, "contract_thirdOccupationStart");
        private Field ttEnd = new Field(6067, "contract_thirdOccupationEnd");

        private Field logoYes = new Field(6070, "contract_logo");
        private Field logoUrl = new Field(6071, "contract_logoDetails");
        private Field agreementDate = new Field(6068L, "contract_agreementDate");

        public Contract(String[] line) {

            this.contractName.value = line[0];
            this.area.value = line[1];
            this.room.value = line[2];
            this.roomType.value = line[3];
            this.bedSize.value = line[4];
            
            this.licenseStartDate.value = line[7];
            this.licenseEndDate.value = line[8];
            
            this.totalAccommodationFee.value = line[9];

            this.mtPaymentDate.value = line[11];
            this.mtPaymentAmount.value = line[12];

            this.htPaymentDate.value = line[13];
            this.htPaymentAmount.value = line[14];

            this.ttPaymentDate.value = line[15];
            this.ttPaymentAmount.value = line[16];

            this.mtStart.value = line[17];
            this.mtEnd.value = line[18];

            this.htStart.value = line[19];
            this.htEnd.value = line[20];

            this.ttStart.value = line[21];
            this.ttEnd.value = line[22];

            this.logoYes.value = line[23];
            this.logoUrl.value = line[24];
            this.agreementDate.value = line[25];
        }
        
//        private String String inputDate) {
//            return dtfOut.print(formatter.parseDateTime(inputDate));
//        }
    }

    
    
    @Data
    class Field {
        private long id;
        private String value;
        private String name;
        private List<Field> nestedFields = null;

        public Field(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private long createContract(String[] line) throws ApiException {

        Contract contract = new Contract(line);

        // Contract fields.

        ContractApi apiInstance = new ContractApi();
        CreateContractRequestDTO contractRequest = new CreateContractRequestDTO();

        List<ContractFieldRequestDTO> fields = new ArrayList<>();

        // Contract name
        ContractFieldRequestDTO contractName = createFieldRequest(contract.getContractName());
        ContractFieldRequestDTO area = createFieldRequest(contract.getArea());
        ContractFieldRequestDTO room = createFieldRequest(contract.getRoom());
        ContractFieldRequestDTO roomType = createFieldRequest(contract.getRoomType());
        ContractFieldRequestDTO bedSize = createFieldRequest(contract.getBedSize());

        ContractFieldRequestDTO licenseStartDate = createFieldRequest(contract.getLicenseStartDate());
        ContractFieldRequestDTO licenseEndDate = createFieldRequest(contract.getLicenseEndDate());

        ContractFieldRequestDTO totalAccommodationFee = createFieldRequest(contract.getTotalAccommodationFee());

        ContractFieldRequestDTO mtPaymentDate = createFieldRequest(contract.getMtPaymentDate());
        ContractFieldRequestDTO mtPaymentAmount = createFieldRequest(contract.getMtPaymentAmount());

        ContractFieldRequestDTO htPaymentDate = createFieldRequest(contract.getHtPaymentDate());
        ContractFieldRequestDTO htPaymentAmount = createFieldRequest(contract.getHtPaymentAmount());

        ContractFieldRequestDTO ttPaymentDate = createFieldRequest(contract.getTtPaymentDate());
        ContractFieldRequestDTO ttPaymentAmount = createFieldRequest(contract.getTtPaymentAmount());

        ContractFieldRequestDTO mtStart = createFieldRequest(contract.getMtStart());
        ContractFieldRequestDTO mtEnd = createFieldRequest(contract.getMtEnd());

        ContractFieldRequestDTO htStart = createFieldRequest(contract.getHtStart());
        ContractFieldRequestDTO htEnd = createFieldRequest(contract.getHtEnd());

        ContractFieldRequestDTO ttStart = createFieldRequest(contract.getTtStart());
        ContractFieldRequestDTO ttEnd = createFieldRequest(contract.getTtEnd());

        ContractFieldRequestDTO logoYes = createFieldRequest(contract.getLogoYes());
        ContractFieldRequestDTO logoUrl = createFieldRequest(contract.getLogoUrl());

        logoYes.addNestedFieldsItem(logoUrl);

        ContractFieldRequestDTO agreementDate = createFieldRequest(contract.getAgreementDate());

        List<ContractFieldRequestDTO> allFields = Arrays.asList(contractName, area, room, roomType, bedSize,
                licenseStartDate, licenseEndDate, totalAccommodationFee, mtPaymentDate, mtPaymentAmount, htPaymentDate,
                htPaymentAmount, ttPaymentDate, ttPaymentAmount, mtStart, mtEnd, htStart, htEnd, ttStart, ttEnd,
                logoYes, agreementDate);

        fields.addAll(allFields);

        contractRequest.setCreatedByUserId(6749541852251925L);
        contractRequest.setTemplateId(CONTRACT_TEMPLATE_ID);
        contractRequest.setFields(fields);

        Link result = apiInstance.postContractUsingPOST(contractRequest, TEAM_ID);
        log.info(result.getHref());
        String id = result.getHref().split("/")[5];
        return Long.parseLong(id);
    }

    private ContractFieldRequestDTO createFieldRequest(Field field) {
        ContractFieldRequestDTO bedSizeField = new ContractFieldRequestDTO();

        bedSizeField.setId(field.getId());
        bedSizeField.setValue(field.getValue());
        bedSizeField.setName(field.getName());

        return bedSizeField;
    }

    private long createUser(String[] line) throws ApiException {

        // Identity fields.
        String studentFirstName = line[5]; // 2776
        String studentLastName = line[6]; // 2778
        String email = line[10];

        UserApi apiInstance = new UserApi();
        CreateUserRequestDTO userRequest = new CreateUserRequestDTO(); // CreateUserRequestDTO | createUserRequestDTO

        userRequest.setFirstName(studentFirstName);
        userRequest.setLastName(studentLastName);
        userRequest.setEmail(email);

        Link result = apiInstance.createUserUsingPOST(userRequest);

        log.info(result.getHref());

        String id = result.getHref().split("/")[5];

        return Long.parseLong(id);
    }
}
