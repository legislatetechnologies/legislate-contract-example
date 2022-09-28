package com.legislate.demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.legislate.ApiException;
import com.legislate.Configuration;
import com.legislate.api.*;
import com.legislate.auth.OAuth;
import com.legislate.model.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opencsv.exceptions.CsvException;

import com.legislate.ApiClient;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/create")
@Slf4j
public class ImportRestController {

    @GetMapping
    public void importContracts() throws IOException, CsvException {
        Configuration configuration = new Configuration("https://sandbox.legislate.tech");
        ApiClient defaultClient = configuration.getDefaultApiClient();

        // Configure OAuth2 access token for authorization: auth0_jwt
        OAuth auth0_jwt = (OAuth) defaultClient.getAuthentication("auth0_jwt");
        auth0_jwt.setAccessToken("");

        UserApi userApi = new UserApi();
        TeamApi teamApi = new TeamApi();
        ContractTypeApi contractTypeApi = new ContractTypeApi();
        ContractTypeTemplateApi contractTypeTemplateApi = new ContractTypeTemplateApi();
        ContractApi contractApi = new ContractApi();
        CollaboratorApi collaboratorApi = new CollaboratorApi();

        try {

            // Create landlord user.
            CreateUserRequestDTO landlord = new CreateUserRequestDTO();
            landlord.setFirstName("John");
            landlord.setLastName("Doe");
            landlord.setEmail("john@doe.co.uk");

            // Create tenant user.
            CreateUserRequestDTO tenant = new CreateUserRequestDTO();
            tenant.setFirstName("Louis");
            tenant.setLastName("Litt");
            tenant.setEmail("marylin@reina.co.uk");

            Link landlordResult = userApi.createUserUsingPOST(landlord);
            Link tenantResult = userApi.createUserUsingPOST(tenant);

            CreateTeamRequestDTO team = new CreateTeamRequestDTO();
            team.setName("Personal team");

            //  landlordResult.getId();
            long landlordUserId = 0;
            long tenantUserId = 0;

            // Create a team for the landlord.
            TeamResponseDTO response = teamApi.postTeamUsingPOST(team, landlordUserId);

            long teamId = response.getId();

            // Get the contract type that you're interested in from the list.

            List<ContractTypeResponseDTO> contractTypes = contractTypeApi.getContractTypesUsingGET();

            // e.g. get the first one
            long assuredShortholdTenancyAgreement = contractTypes.get(0).getId();

            // Get the templates that you're interested in from the list.
            List<ContractTypeTemplateResponseDTO> templates = contractTypeTemplateApi.getTemplatesUsingGET(assuredShortholdTenancyAgreement, teamId);

            // e.g. get the first one
            long v7 = templates.get(0).getId();

            // Postman: Get the fields that need to be submitted.
            // GET /v1/templates/{templateId}/terms
            // See resources/contract_fields.json

            // Create contract.
            CreateContractRequestDTO createContractRequestDTO = new CreateContractRequestDTO();

            createContractRequestDTO.templateId(v7);
            createContractRequestDTO.createdByUserId(landlordUserId);

            // Now we need to fill the fields to the request object.
            ContractFieldRequestDTO contractName = new ContractFieldRequestDTO();
            contractName.id(3775L);
            contractName.value("Assured Shorthold Tenancy Agreement - John Doe and Louis Litt");

            ContractFieldRequestDTO address = new ContractFieldRequestDTO();
            address.id(3776L);
            address.value("55 Bayswater Road, W2 3SH");

            ContractFieldRequestDTO mortgage = new ContractFieldRequestDTO();
            mortgage.id(3778L);
            mortgage.value("mortgageNo");

            // Dependent fields - making a decision.
            ContractFieldRequestDTO rentFrequency = new ContractFieldRequestDTO();
            rentFrequency.id(3779L);
            rentFrequency.value("month");

            ContractFieldRequestDTO breakClause = new ContractFieldRequestDTO();
            breakClause.id(3860L);
            breakClause.value("breakClauseNo");

            rentFrequency.nestedFields(Arrays.asList(breakClause));

            // End mapping fields.

            List<ContractFieldRequestDTO> fieldsWithValues =
                Arrays.asList(contractName, address, mortgage, rentFrequency);


            createContractRequestDTO.fields(fieldsWithValues);

            Link contractResponse = contractApi.postContractUsingPOST(createContractRequestDTO, teamId);

            // Get the contractId from the URI in the contractResponse.
            long contractId = 0;

            // Postman: Get landlord and tenant identity fields that need to be filled in.
            // /v1/templates/149/fields?roles=party&side=first
            // /v1/templates/149/fields?roles=party&side=second
            // See resources/identity_fields_landlord.json and resources/identity_fields_tenant.json

            // Create landlord collaborator.
            CollaboratorFieldsRequestDTO landlordCollaborator = new CollaboratorFieldsRequestDTO();
            landlordCollaborator.side(CollaboratorFieldsRequestDTO.SideEnum.FIRST);
            landlordCollaborator.role(Arrays.asList(CollaboratorFieldsRequestDTO.RoleEnum.PARTY));
            landlordCollaborator.userId(landlordUserId);

            // We fill in the landlord identity fields.
            CreateCollaboratorIdentityFieldV1DTO landlordFirstName = new CreateCollaboratorIdentityFieldV1DTO();
            landlordFirstName.id(2117L);
            landlordFirstName.value("Joh");

            CreateCollaboratorIdentityFieldV1DTO landlordLastName = new CreateCollaboratorIdentityFieldV1DTO();
            landlordLastName.id(2118L);
            landlordLastName.value("Doe");

            CreateCollaboratorIdentityFieldV1DTO landlordEmail = new CreateCollaboratorIdentityFieldV1DTO();
            landlordEmail.id(2119L);
            landlordEmail.value("john@doe.co.uk");

            CreateCollaboratorIdentityFieldV1DTO landlordAddress = new CreateCollaboratorIdentityFieldV1DTO();
            landlordAddress.id(2120L);
            landlordAddress.value("11 Oxford Street, W1 4HJ");

            landlordCollaborator.setIdentityFields(Arrays.asList(landlordFirstName, landlordLastName, landlordEmail, landlordAddress));

            // End filling in the landlord identity fields.
            Link landlordLink = collaboratorApi.postCollaboratorUsingPOST(landlordCollaborator, contractId);
            long landlordId = 0;

            // Create tenant collaborator.
            CollaboratorFieldsRequestDTO tenantCollaborator = new CollaboratorFieldsRequestDTO();
            tenantCollaborator.side(CollaboratorFieldsRequestDTO.SideEnum.SECOND);
            tenantCollaborator.role(Arrays.asList(CollaboratorFieldsRequestDTO.RoleEnum.PARTY));
            tenantCollaborator.userId(tenantUserId);

            // We fill in the tenant identity fields.
            CreateCollaboratorIdentityFieldV1DTO tenantFirstName = new CreateCollaboratorIdentityFieldV1DTO();
            tenantFirstName.id(2112L);
            tenantFirstName.value("Louis");


            CreateCollaboratorIdentityFieldV1DTO tenantLasttName = new CreateCollaboratorIdentityFieldV1DTO();
            tenantLasttName.id(2113L);
            tenantLasttName.value("Litt");

            // ...

            CreateCollaboratorIdentityFieldV1DTO tenantMobile = new CreateCollaboratorIdentityFieldV1DTO();
            tenantMobile.id(2116L);
            tenantMobile.value("07456565656");

            CreateCollaboratorIdentityFieldV1DTO tenantOrGuarantoor = new CreateCollaboratorIdentityFieldV1DTO();
            tenantOrGuarantoor.id(2121L);
            tenantOrGuarantoor.value("tenant");

            tenantCollaborator.setIdentityFields(Arrays.asList(tenantFirstName, tenantLasttName, tenantMobile, tenantOrGuarantoor));
            // End filling in the tenant identity fields.

            Link tenantLink = collaboratorApi.postCollaboratorUsingPOST(tenantCollaborator, contractId);
            long tenantId = 0;

            // Now it's time to sign the contract.
            collaboratorApi.updateCollaboratorBinaryFileSignatureUsingPATCH(tenantId);
            collaboratorApi.updateCollaboratorBinaryFileSignatureUsingPATCH(landlordId);

            // Download the contract PDF.
            byte[] pdf = contractApi.getContractFileUsingGET(contractId);

        } catch (ApiException e) {
            System.err.println("Exception when calling UserApi#getUsersUsingGET");
            e.printStackTrace();
        }
    }
}
