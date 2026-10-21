package com.cafe.management.config;

import com.cafe.management.entity.Category;
import com.cafe.management.entity.Product;
import com.cafe.management.entity.Role;
import com.cafe.management.entity.User;
import com.cafe.management.entity.UserStatus;
import com.cafe.management.repository.CategoryRepository;
import com.cafe.management.repository.ProductRepository;
import com.cafe.management.repository.UserRepository;
import com.cafe.management.util.StaffCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Runs once on application startup.
 *
 * <ol>
 *   <li><b>First admin bootstrap</b> - if no ADMIN account exists yet, one is created
 *       using the credentials from {@code app.admin.email} / {@code app.admin.password}
 *       (overridable through the ADMIN_EMAIL / ADMIN_PASSWORD environment variables).
 *       The password is never written to the logs.</li>
 *   <li><b>Demo staff account</b> - creates one STAFF login the first time so the
 *       role-based authorisation can be exercised immediately.</li>
 *   <li><b>Sample data</b> - inserts a few categories and products the first time the
 *       database is empty so the UI is not blank.</li>
 * </ol>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@cafe.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.staff.email:ravi@cafe.com}")
    private String staffEmail;

    @Value("${app.staff.password:Staff@123}")
    private String staffPassword;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        bootstrapAdmin();
        seedDemoStaff();
        backfillStaffCodes();
        seedSampleData();
    }

    /**
     * Gives every account that predates the counter PIN feature a badge number.
     * New accounts get theirs in {@code UserServiceImpl.createUser}.
     */
    private void backfillStaffCodes() {
        List<User> withoutCode = userRepository.findAll().stream()
                .filter(user -> user.getStaffCode() == null)
                .toList();
        if (withoutCode.isEmpty()) {
            return;
        }
        withoutCode.forEach(user -> user.setStaffCode(StaffCodes.forUserId(user.getId())));
        userRepository.saveAll(withoutCode);
        log.info("Assigned counter badge numbers to {} existing account(s).", withoutCode.size());
    }

    private void bootstrapAdmin() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name("Cafe Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        log.info("Bootstrapped first ADMIN account: {}. Please change the password after first login.", adminEmail);
    }

    private void seedDemoStaff() {
        if (userRepository.countByRole(Role.STAFF) > 0 || userRepository.existsByEmailIgnoreCase(staffEmail)) {
            return;
        }
        User staff = User.builder()
                .name("Ravi Staff")
                .email(staffEmail)
                .password(passwordEncoder.encode(staffPassword))
                .role(Role.STAFF)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(staff);
        log.info("Seeded a demo STAFF account: {}. Please change the password after first login.", staffEmail);
    }

    private void seedSampleData() {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category coffee = categoryRepository.save(Category.builder().name("Coffee").description("Hot coffee based drinks").build());
        Category tea = categoryRepository.save(Category.builder().name("Tea").description("Hot tea based drinks").build());
        Category snacks = categoryRepository.save(Category.builder().name("Snacks").description("Light bites and savouries").build());
        Category desserts = categoryRepository.save(Category.builder().name("Desserts").description("Sweet treats").build());
        Category cold = categoryRepository.save(Category.builder().name("Cold Beverages").description("Chilled drinks").build());

        productRepository.saveAll(List.of(
                product("Cappuccino", "Espresso with steamed milk foam", "120.00", coffee),
                product("Latte", "Espresso with lots of steamed milk", "140.00", coffee),
                product("Espresso", "Strong black coffee shot", "90.00", coffee),
                product("Masala Tea", "Spiced Indian tea", "40.00", tea),
                product("Green Tea", "Light and refreshing", "50.00", tea),
                product("Veg Sandwich", "Grilled vegetable sandwich", "100.00", snacks),
                product("French Fries", "Crispy salted fries", "110.00", snacks),
                product("Chocolate Brownie", "Warm fudge brownie", "130.00", desserts),
                product("Cheesecake", "New York style cheesecake", "160.00", desserts),
                product("Cold Coffee", "Iced blended coffee", "150.00", cold),
                product("Lemon Iced Tea", "Chilled lemon tea", "90.00", cold)
        ));
        log.info("Seeded {} sample categories and {} sample products.", 5, 11);
    }

    private Product product(String name, String description, String price, Category category) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .category(category)
                .availability(true)
                .build();
    }
}
