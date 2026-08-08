package com.bdreview.platform.business;

import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.PhoneNumberUtils;
import com.bdreview.platform.common.ResourceNotFoundException;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BusinessService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final AreaRepository areaRepository;
    private final BusinessAttributeRepository attributeRepository;

    public BusinessService(BusinessRepository businessRepository,
                            CategoryRepository categoryRepository,
                            CityRepository cityRepository,
                            AreaRepository areaRepository,
                            BusinessAttributeRepository attributeRepository) {
        this.businessRepository = businessRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
        this.attributeRepository = attributeRepository;
    }

    @Transactional
    public Business create(UUID ownerUserId, CreateBusinessRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("Area not found"));

        Set<BusinessAttribute> attributes = request.attributeIds() == null ? new HashSet<>()
                : new HashSet<>(attributeRepository.findByIdIn(request.attributeIds()));

        Business business = Business.builder()
                .ownerUserId(ownerUserId)
                .name(request.name())
                .slug(generateUniqueSlug(request.name()))
                .category(category)
                .city(city)
                .area(area)
                .contactNumber(PhoneNumberUtils.normalize(request.contactNumber()))
                .operatingHours(request.operatingHours())
                .description(request.description())
                .coverPhotoUrl(request.coverPhotoUrl())
                .location(point(request.latitude(), request.longitude()))
                .priceTier(request.priceTier())
                .attributes(attributes)
                .build();

        return businessRepository.save(business);
    }

    @Transactional
    public Business update(UUID requesterUserId, UUID businessId, UpdateBusinessRequest request) {
        Business business = getOwnedOrThrow(requesterUserId, businessId);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("Area not found"));
        Set<BusinessAttribute> attributes = request.attributeIds() == null ? new HashSet<>()
                : new HashSet<>(attributeRepository.findByIdIn(request.attributeIds()));

        business.setName(request.name());
        business.setCategory(category);
        business.setCity(city);
        business.setArea(area);
        business.setContactNumber(PhoneNumberUtils.normalize(request.contactNumber()));
        business.setOperatingHours(request.operatingHours());
        business.setDescription(request.description());
        business.setCoverPhotoUrl(request.coverPhotoUrl());
        business.setLocation(point(request.latitude(), request.longitude()));
        business.setPriceTier(request.priceTier());
        business.setAttributes(attributes);
        // slug is immutable by design (spec §1) — never regenerated on update.

        return businessRepository.save(business);
    }

    @Transactional
    public void softDelete(UUID requesterUserId, UUID businessId) {
        getOwnedOrThrow(requesterUserId, businessId);
        businessRepository.softDelete(businessId);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBySlug(String slug) {
        Business business = businessRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + slug));
        return BusinessResponse.from(business);
    }

    @Transactional(readOnly = true)
    public Page<BusinessResponse> search(UUID categoryId, UUID areaId, String priceTier, Double minRating,
                                          Double lat, Double lng, Double radiusMeters, String sort,
                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, com.bdreview.platform.common.PageRequestDefaults.clamp(size));
        return businessRepository.search(categoryId, areaId, priceTier, minRating, lat, lng, radiusMeters, sort, pageable)
                .map(BusinessResponse::from);
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> myBusinesses(UUID ownerUserId) {
        return businessRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .stream().map(BusinessResponse::from).toList();
    }

    private Business getOwnedOrThrow(UUID requesterUserId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerUserId().equals(requesterUserId)) {
            throw new ForbiddenException("You do not own this business listing");
        }
        return business;
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "business";
        }
        String slug = base;
        while (businessRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            slug = base + "-" + randomSuffix();
        }
        return slug;
    }

    private static String randomSuffix() {
        return Integer.toHexString(RANDOM.nextInt(0xFFFFFF));
    }

    private static Point point(double latitude, double longitude) {
        Point p = GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(longitude, latitude));
        p.setSRID(4326);
        return p;
    }
}
