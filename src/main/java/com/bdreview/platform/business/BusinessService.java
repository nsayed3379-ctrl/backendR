package com.bdreview.platform.business;

import com.bdreview.platform.auth.User;
import com.bdreview.platform.auth.UserRepository;
import com.bdreview.platform.auth.UserRole;
import com.bdreview.platform.common.BadRequestException;
import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.PhoneNumberUtils;
import com.bdreview.platform.common.ResourceNotFoundException;
import com.bdreview.platform.gallery.BusinessPhoto;
import com.bdreview.platform.gallery.BusinessPhotoRepository;
import com.bdreview.platform.notification.NotificationChannel;
import com.bdreview.platform.notification.NotificationService;
import com.bdreview.platform.notification.NotificationType;
import com.bdreview.platform.review.VoteType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final BusinessPhotoRepository businessPhotoRepository;
    private final BusinessReactionRepository businessReactionRepository;
    private final BusinessService self;

    public BusinessService(BusinessRepository businessRepository,
                            CategoryRepository categoryRepository,
                            CityRepository cityRepository,
                            AreaRepository areaRepository,
                            BusinessAttributeRepository attributeRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            BusinessPhotoRepository businessPhotoRepository,
                            BusinessReactionRepository businessReactionRepository,
                            @Lazy BusinessService self) {
        this.businessRepository = businessRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
        this.attributeRepository = attributeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.businessPhotoRepository = businessPhotoRepository;
        this.businessReactionRepository = businessReactionRepository;
        this.self = self;
    }

    private static final int[] ZERO_REACTIONS = new int[]{0, 0, 0};

    @Transactional
    public BusinessResponse create(UUID ownerUserId, CreateBusinessRequest request) {
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
                .logoUrl(request.logoUrl())
                .location(point(request.latitude(), request.longitude()))
                .priceTier(request.priceTier())
                .attributes(attributes)
                .build();

        Business saved = businessRepository.save(business);
        return BusinessResponse.from(saved, photoUrlsFor(saved, List.of()), ZERO_REACTIONS);
    }

    @Transactional
    public BusinessResponse update(UUID requesterUserId, UUID businessId, UpdateBusinessRequest request) {
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
        business.setLogoUrl(request.logoUrl());
        business.setLocation(point(request.latitude(), request.longitude()));
        business.setPriceTier(request.priceTier());
        business.setAttributes(attributes);
        // slug is immutable by design (spec §1) — never regenerated on update.

        Business saved = businessRepository.save(business);
        List<String> galleryUrls = businessPhotoRepository.findByBusinessIdOrderBySortOrderAsc(saved.getId())
                .stream().map(BusinessPhoto::getUrl).toList();
        return BusinessResponse.from(saved, photoUrlsFor(saved, galleryUrls), reactionTotalsFor(saved.getId()));
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
        List<String> galleryUrls = businessPhotoRepository.findByBusinessIdOrderBySortOrderAsc(business.getId())
                .stream().map(BusinessPhoto::getUrl).toList();
        return BusinessResponse.from(business, photoUrlsFor(business, galleryUrls), reactionTotalsFor(business.getId()));
    }

    @Transactional(readOnly = true)
    public Page<BusinessResponse> search(UUID categoryId, UUID areaId, String priceTier, Double minRating,
                                          Double lat, Double lng, Double radiusMeters, String sort,
                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, com.bdreview.platform.common.PageRequestDefaults.clamp(size));
        Page<Business> results = businessRepository.search(categoryId, areaId, priceTier, minRating, lat, lng, radiusMeters, sort, pageable);
        Map<UUID, List<String>> galleryByBusiness = galleryUrlsByBusiness(results.getContent());
        Map<UUID, int[]> reactionsByBusiness = reactionTotalsByBusiness(results.getContent());
        return results.map(b -> BusinessResponse.from(b,
                photoUrlsFor(b, galleryByBusiness.getOrDefault(b.getId(), List.of())),
                reactionsByBusiness.getOrDefault(b.getId(), ZERO_REACTIONS)));
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> myBusinesses(UUID ownerUserId) {
        List<Business> businesses = businessRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId);
        Map<UUID, List<String>> galleryByBusiness = galleryUrlsByBusiness(businesses);
        Map<UUID, int[]> reactionsByBusiness = reactionTotalsByBusiness(businesses);
        return businesses.stream()
                .map(b -> BusinessResponse.from(b,
                        photoUrlsFor(b, galleryByBusiness.getOrDefault(b.getId(), List.of())),
                        reactionsByBusiness.getOrDefault(b.getId(), ZERO_REACTIONS)))
                .toList();
    }

    /** Batched gallery lookup grouped by business id — one query instead of one per business. */
    private Map<UUID, List<String>> galleryUrlsByBusiness(List<Business> businesses) {
        List<UUID> ids = businesses.stream().map(Business::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<String>> byBusiness = new HashMap<>();
        for (BusinessPhoto photo : businessPhotoRepository.findByBusinessIdInOrderBySortOrderAsc(ids)) {
            byBusiness.computeIfAbsent(photo.getBusinessId(), k -> new ArrayList<>()).add(photo.getUrl());
        }
        return byBusiness;
    }

    /** Batched useful/funny/cool counts grouped by business id — one query instead of one per business. */
    private Map<UUID, int[]> reactionTotalsByBusiness(List<Business> businesses) {
        List<UUID> ids = businesses.stream().map(Business::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, int[]> byBusiness = new HashMap<>();
        for (Object[] row : businessReactionRepository.countGroupedByBusinessIds(ids)) {
            UUID businessId = (UUID) row[0];
            VoteType type = (VoteType) row[1];
            int count = ((Number) row[2]).intValue();
            int[] totals = byBusiness.computeIfAbsent(businessId, k -> new int[]{0, 0, 0});
            totals[indexOf(type)] = count;
        }
        return byBusiness;
    }

    private int[] reactionTotalsFor(UUID businessId) {
        return reactionTotalsByBusiness(List.of(Business.builder().id(businessId).build()))
                .getOrDefault(businessId, ZERO_REACTIONS);
    }

    private static int indexOf(VoteType type) {
        return switch (type) {
            case USEFUL -> 0;
            case FUNNY -> 1;
            case COOL -> 2;
        };
    }

    /**
     * Toggles a business-level reaction (add if absent, remove if present) — mirrors
     * review.ReviewService.vote()'s toggle behavior, one row per (business, user, type).
     */
    @Transactional
    public void react(UUID userId, UUID businessId, VoteType type) {
        if (businessReactionRepository.existsByBusinessIdAndUserIdAndReactionType(businessId, userId, type)) {
            businessReactionRepository.deleteByBusinessIdAndUserIdAndReactionType(businessId, userId, type);
        } else {
            businessReactionRepository.save(BusinessReaction.builder()
                    .businessId(businessId).userId(userId).reactionType(type).build());
        }
    }

    /** Card carousel source: cover photo first (if set), then gallery photos, de-duplicated. */
    private List<String> photoUrlsFor(Business business, List<String> galleryUrls) {
        Set<String> ordered = new LinkedHashSet<>();
        if (business.getCoverPhotoUrl() != null && !business.getCoverPhotoUrl().isBlank()) {
            ordered.add(business.getCoverPhotoUrl());
        }
        ordered.addAll(galleryUrls);
        return ordered.stream().collect(Collectors.toList());
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
