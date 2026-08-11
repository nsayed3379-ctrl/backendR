package com.bdreview.platform.business;

import com.bdreview.platform.auth.User;
import com.bdreview.platform.auth.UserRepository;
import com.bdreview.platform.auth.UserRole;
import com.bdreview.platform.common.BadRequestException;
import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.PhoneNumberUtils;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.notification.NotificationChannel;
import com.bdreview.platform.notification.NotificationService;
import com.bdreview.platform.notification.NotificationType;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
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
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BusinessService self;

    public BusinessService(BusinessRepository businessRepository,
                            CategoryRepository categoryRepository,
                            CityRepository cityRepository,
                            AreaRepository areaRepository,
                            BusinessAttributeRepository attributeRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            @Lazy BusinessService self) {
        this.businessRepository = businessRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
        this.attributeRepository = attributeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.self = self;
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

    /**
     * Report workflow: the "next step" for an owner after their listing was flagged (spec:
     * a flag shouldn't be a dead end). Doesn't clear the flag itself — only an admin can do
     * that, from the Businesses admin screen, after actually looking at the situation — this
     * just makes sure every admin gets told the owner is asking for that look.
     */
    @Transactional
    public void requestFlagReview(UUID requesterUserId, UUID businessId) {
        Business business = getOwnedOrThrow(requesterUserId, businessId);
        if (!business.isFlagged()) {
            throw new BadRequestException("This listing is not currently flagged.");
        }
        self.notifyAdminsOfFlagReviewRequest(businessId, business.getName());
    }

    @Async
    public void notifyAdminsOfFlagReviewRequest(UUID businessId, String businessName) {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN, Pageable.unpaged()).getContent();
        for (User admin : admins) {
            notificationService.create(admin.getId(), NotificationType.FLAG_REVIEW_REQUESTED,
                    "Flag review requested",
                    "The owner of \"" + businessName + "\" has requested a review of their flagged listing.",
                    "BUSINESS", businessId, NotificationChannel.IN_APP);
        }
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
