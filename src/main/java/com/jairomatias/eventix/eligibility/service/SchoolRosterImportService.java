package com.jairomatias.eventix.eligibility.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.RosterImportError;
import com.jairomatias.eventix.eligibility.dto.RosterImportResult;
import com.jairomatias.eventix.eligibility.dto.RosterImportRow;
import com.jairomatias.eventix.eligibility.entity.PromotionMember;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.entity.SchoolRosterImport;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolRosterImportRepository;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.service.InstitutionAuthorizationService;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolRosterImportService {

    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    private final CsvRosterParser csvRosterParser;
    private final PersonNameNormalizer nameNormalizer;
    private final PromotionMemberRepository promotionMemberRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final SchoolRosterImportRepository rosterImportRepository;
    private final UserRepository userRepository;
    private final InstitutionAuthorizationService authorizationService;

    public SchoolRosterImportService(
            CsvRosterParser csvRosterParser,
            PersonNameNormalizer nameNormalizer,
            PromotionMemberRepository promotionMemberRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            SchoolRosterImportRepository rosterImportRepository,
            UserRepository userRepository,
            InstitutionAuthorizationService authorizationService) {
        this.csvRosterParser = csvRosterParser;
        this.nameNormalizer = nameNormalizer;
        this.promotionMemberRepository = promotionMemberRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.rosterImportRepository = rosterImportRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public RosterImportResult importCsv(
            Long promotionId,
            Long importedById,
            String sourceName,
            byte[] content) {
        validateFile(sourceName, content);

        SchoolPromotion promotion = schoolPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("La promoción indicada no existe."));
        User importedBy = userRepository.findById(importedById)
                .orElseThrow(() -> new IllegalArgumentException("El usuario importador no existe."));
        authorizationService.requireOperationalRole(
                promotion.getInstitution(),
                importedById,
                InstitutionMembershipRole.OWNER,
                InstitutionMembershipRole.ADMIN,
                InstitutionMembershipRole.ROSTER_MANAGER);
        if (!promotion.isActive()) {
            throw new IllegalArgumentException(
                    "La promoción debe estar activa para importar el padrón.");
        }

        String checksum = checksum(content);
        if (rosterImportRepository.existsByPromotion_IdAndFileChecksum(promotionId, checksum)) {
            throw new IllegalArgumentException(
                    "Este archivo ya fue importado previamente para la promoción seleccionada.");
        }

        List<RosterImportRow> rows = csvRosterParser.parse(content);
        List<PromotionMember> accepted = new ArrayList<>();
        List<RosterImportError> errors = new ArrayList<>();
        Set<String> seenRows = existingRowKeys(promotionId);

        for (RosterImportRow row : rows) {
            try {
                validateRow(row);
                String rowKey = rowKey(row.fullName(), row.studentCode());
                if (!seenRows.add(rowKey)) {
                    throw new IllegalArgumentException(
                            "La misma persona y código estudiantil ya existen en este padrón.");
                }
                accepted.add(new PromotionMember(
                        promotion,
                        row.fullName().trim(),
                        trimToNull(row.studentCode()),
                        trimToNull(row.sourceReference())));
            } catch (IllegalArgumentException exception) {
                errors.add(new RosterImportError(row.rowNumber(), exception.getMessage()));
            }
        }

        promotionMemberRepository.saveAll(accepted);
        SchoolRosterImport rosterImport = rosterImportRepository.save(new SchoolRosterImport(
                promotion,
                sourceName.trim(),
                checksum,
                importedBy,
                rows.size(),
                accepted.size(),
                errors.size()));

        return new RosterImportResult(
                rosterImport.getId(),
                rows.size(),
                accepted.size(),
                errors.size(),
                List.copyOf(errors));
    }

    private Set<String> existingRowKeys(Long promotionId) {
        Set<String> keys = new HashSet<>();
        promotionMemberRepository.findAllByPromotion_IdOrderByFullNameAsc(promotionId)
                .forEach(member -> keys.add(rowKey(member.getFullName(), member.getStudentCode())));
        return keys;
    }

    private String rowKey(String fullName, String studentCode) {
        String code = studentCode == null ? "" : studentCode.trim().toUpperCase();
        return nameNormalizer.normalize(fullName) + "|" + code;
    }

    private void validateFile(String sourceName, byte[] content) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la fuente del padrón.");
        }
        if (sourceName.trim().length() > 240) {
            throw new IllegalArgumentException("La fuente del padrón excede 240 caracteres.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo del padrón está vacío.");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("El archivo del padrón no puede superar 5 MB.");
        }
    }

    private void validateRow(RosterImportRow row) {
        if (row.fullName() == null || row.fullName().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (row.fullName().trim().length() > 180) {
            throw new IllegalArgumentException("El nombre completo excede 180 caracteres.");
        }
        if (row.studentCode() != null && row.studentCode().trim().length() > 80) {
            throw new IllegalArgumentException("El código estudiantil excede 80 caracteres.");
        }
        if (row.sourceReference() != null && row.sourceReference().trim().length() > 240) {
            throw new IllegalArgumentException("La referencia de origen excede 240 caracteres.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String checksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
