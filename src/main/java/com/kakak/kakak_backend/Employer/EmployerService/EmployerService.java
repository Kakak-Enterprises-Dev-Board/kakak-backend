package com.kakak.kakak_backend.Employer.EmployerService;

import com.kakak.kakak_backend.Employer.EmployerDTO.EmployerAddressRequest;
import com.kakak.kakak_backend.Employer.EmployerDTO.EmployerDocumentRequest;
import com.kakak.kakak_backend.Employer.EmployerDTO.EmployerProfileResponse;
import com.kakak.kakak_backend.Employer.EmployerDTO.PublicEmployerResponse;
import com.kakak.kakak_backend.Employer.EmployerDTO.UpdateEmployerProfileRequest;
import com.kakak.kakak_backend.Employer.EmployerEntity.Employer;
import com.kakak.kakak_backend.Employer.EmployerEntity.Employer_Addresses;
import com.kakak.kakak_backend.Employer.EmployerEntity.Employer_Documents;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import com.kakak.kakak_backend.Employer.EmployerRepository.EmployerAddressRepository;
import com.kakak.kakak_backend.Employer.EmployerRepository.EmployerDocumentRepository;
import com.kakak.kakak_backend.Employer.EmployerRepository.EmployerRepository;
import com.kakak.kakak_backend.Files.fileEntity.FileModule;
import com.kakak.kakak_backend.Files.fileEntity.files;
import com.kakak.kakak_backend.Files.fileService.FileStorageService;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final EmployerAddressRepository employerAddressRepository;
    private final EmployerDocumentRepository employerDocumentRepository;
    private final UsersRepo usersRepo;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PublicEmployerResponse getPublicEmployer(UUID id) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employer not found"));
        return toPublicEmployerResponse(employer);
    }

    @Transactional
    public EmployerProfileResponse updateEmployerProfile(UpdateEmployerProfileRequest request, Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        AuthUsers user = currentUser(authentication);

        Employer employer =
                employerRepository.findByUserId(user.getId())
                        .stream()
                        .findFirst()
                        .orElseGet(Employer::new);
        if (employer.getId() == null) {
            employer.setUser_id(user);
            employer.setVerificationStatus(VerificationStatus.PENDING);
        }

        employer.setCompany_name(request.getCompanyName());
        employer.setRegistration_number(request.getRegistrationNumber());
        employer.setCompany_size(request.getCompanySize());
        employer.setCompany_description(request.getCompanyDescription());
        employer.setIndustry(request.getIndustry());
        employer.setCompany_website(request.getCompanyWebsite());

        return toProfileResponse(employerRepository.save(employer), null, null);
    }

    @Transactional
    public EmployerProfileResponse upsertEmployerAddress(EmployerAddressRequest request, Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        Employer employer = currentEmployer(authentication);

        Employer_Addresses address = employerAddressRepository.findByEmployerId(employer.getId())
                .orElseGet(Employer_Addresses::new);
        address.setEmployer_id(employer);
        address.setAddress_line_1(request.getAddressLine1());
        address.setAddress_line_2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPincode(request.getPincode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        return toProfileResponse(employer, employerAddressRepository.save(address), null);
    }

    @Transactional
    public EmployerProfileResponse uploadEmployerDocument(EmployerDocumentRequest request, Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        Employer employer = currentEmployer(authentication);
        fileStorageService.getFile(request.getFileId(), authentication);
        files file = fileStorageService.findFile(request.getFileId());
        if (!FileModule.EMPLOYER_DOCUMENT.name().equals(file.getModule())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an employer document");
        }

        Employer_Documents document = new Employer_Documents();
        document.setEmployer_id(employer);
        document.setFile_id(file);
        document.setDocumentType(request.getDocumentType());
        document.setVerificationStatus(VerificationStatus.PENDING);

        return toProfileResponse(employer, null, employerDocumentRepository.save(document));
    }

    private Employer currentEmployer(Authentication authentication) {
        AuthUsers user = currentUser(authentication);
        return employerRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated employer required"));
    }

    private AuthUsers currentUser(Authentication authentication) {
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return usersRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private EmployerProfileResponse toProfileResponse(
            Employer employer,
            Employer_Addresses address,
            Employer_Documents document) {

        return new EmployerProfileResponse(
                employer.getId(),
                employer.getUser_id().getId(),
                employer.getCompany_name(),
                employer.getRegistration_number(),
                employer.getCompany_size(),
                employer.getCompany_description(),
                employer.getIndustry(),
                employer.getCompany_website(),
                employer.getVerificationStatus(),
                employer.getCreated_at(),
                toAddressResponse(address),
                toDocumentResponse(document)
        );
    }

    private PublicEmployerResponse toPublicEmployerResponse(Employer employer) {
        return new PublicEmployerResponse(
                employer.getId(),
                employer.getCompany_name(),
                employer.getCompany_description(),
                employer.getIndustry(),
                employer.getCompany_website(),
                VerificationStatus.APPROVED.equals(employer.getVerificationStatus()),
                employer.getCreated_at()
        );
    }

    private EmployerProfileResponse.EmployerAddress toAddressResponse(Employer_Addresses address) {
        if (address == null) {
            return null;
        }
        return new EmployerProfileResponse.EmployerAddress(
                address.getId(),
                address.getAddress_line_1(),
                address.getAddress_line_2(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPincode(),
                address.getLatitude(),
                address.getLongitude()
        );
    }

    private EmployerProfileResponse.EmployerDocument toDocumentResponse(Employer_Documents document) {
        if (document == null) {
            return null;
        }
        return new EmployerProfileResponse.EmployerDocument(
                document.getId(),
                document.getFile_id().getId(),
                document.getDocumentType(),
                document.getVerificationStatus(),
                document.getUploaded_at()
        );
    }
}
