# Masukibooks Backend API

A comprehensive e-commerce backend API for an online bookstore, built with Spring Boot 3.3.5 and PostgreSQL/Supabase.

## 🚀 Features

### User Management
- User registration and authentication (email/phone)
- OAuth provider integration support
- OTP-based verification
- User profile management
- Address management
- Session management

### Product Management
- Product catalog with categories
- Multi-language product translations
- Product images
- Inventory tracking with logs
- Product reviews and ratings
- Search and filtering

### Shopping Experience
- Guest and registered user carts
- Discount codes and promotions
- Order management
- Payment integration (Razorpay/Stripe ready)
- Shipment tracking
- Order history

### Admin Features
- Admin user management
- Product CRUD operations
- Order management
- Inventory management
- Review moderation
- Refund processing
- Audit logging

### Additional Features
- Email/SMS notifications
- Multi-language UI translations
- Comprehensive audit trail
- RESTful API design
- API documentation (Swagger/OpenAPI)

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **Database**: PostgreSQL (Supabase)
- **Security**: Spring Security + JWT
- **ORM**: Hibernate/JPA
- **Documentation**: SpringDoc OpenAPI
- **Email**: Spring Mail
- **Build Tool**: Maven

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 14+ or Supabase account
- SMTP server for email notifications (Gmail recommended)

## 🔧 Installation

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd masukibooks-backend
```

### 2. Configure Database

#### Option A: Using Supabase (Recommended for Production)

Follow the detailed guide in [SUPABASE_SETUP.md](SUPABASE_SETUP.md)

#### Option B: Local PostgreSQL

```bash
# Create database
createdb masukibooks

# Run the schema script
psql -d masukibooks -f database-schema.sql
```

### 3. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```properties
# Database (Supabase)
DATABASE_URL=jdbc:postgresql://db.[your-project-ref].supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_supabase_password

# JWT Secret (generate a secure random string)
JWT_SECRET=your_secure_jwt_secret_min_64_characters

# Email Configuration
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Frontend URL
FRONTEND_URL=http://localhost:5173
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

#### Development Mode:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Production Mode:
```bash
java -jar target/masukibooks-backend-1.0.0.jar --spring.profiles.active=prod
```

## 📁 Project Structure

```
masukibooks-backend/
├── src/
│   ├── main/
│   │   ├── java/com/masukibooks/
│   │   │   ├── config/          # Configuration classes
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/      # REST Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── ...
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/         # JPA Entities (24 entities)
│   │   │   ├── repository/     # JPA Repositories
│   │   │   ├── service/        # Business Logic
│   │   │   ├── security/       # Security Components
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtTokenProvider.java
│   │   │   └── exception/      # Exception Handlers
│   │   └── resources/
│   │       ├── application.yml           # Base config
│   │       ├── application-dev.yml       # Dev config
│   │       └── application-prod.yml      # Prod config
│   └── test/                   # Unit and Integration Tests
├── .env.example                # Environment variables template
├── .gitignore                  # Git ignore rules
├── pom.xml                     # Maven dependencies
├── DATABASE_CHECKLIST.md       # Database verification checklist
├── SUPABASE_SETUP.md          # Supabase setup guide
└── README.md                   # This file
```

## 🌐 API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/verify-otp` - OTP verification
- `POST /api/v1/auth/refresh-token` - Refresh JWT token

### Products
- `GET /api/v1/products` - List all products
- `GET /api/v1/products/{id}` - Get product details
- `POST /api/v1/products` - Create product (Admin)
- `PUT /api/v1/products/{id}` - Update product (Admin)
- `DELETE /api/v1/products/{id}` - Delete product (Admin)

### Categories
- `GET /api/v1/categories` - List all categories
- `GET /api/v1/categories/{id}` - Get category details
- `POST /api/v1/categories` - Create category (Admin)

### Cart
- `GET /api/v1/cart` - Get cart
- `POST /api/v1/cart/items` - Add item to cart
- `PUT /api/v1/cart/items/{id}` - Update cart item
- `DELETE /api/v1/cart/items/{id}` - Remove cart item

### Orders
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders` - List user orders
- `GET /api/v1/orders/{id}` - Get order details
- `PUT /api/v1/orders/{id}/status` - Update order status (Admin)

### Reviews
- `POST /api/v1/reviews` - Create review
- `GET /api/v1/products/{id}/reviews` - Get product reviews

### Admin
- `GET /api/v1/admin/users` - List users
- `GET /api/v1/admin/orders` - List all orders
- `GET /api/v1/admin/inventory` - Inventory management

See full API documentation at: `http://localhost:8081/swagger-ui.html`

## 🔐 Security

- JWT-based authentication
- BCrypt password hashing
- Role-based access control (USER, ADMIN, SUPER_ADMIN, MODERATOR)
- CORS configuration
- XSS protection
- CSRF protection (disabled for API)

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn clean test jacoco:report
```

## 📚 API Documentation

Access Swagger UI at:
- Development: http://localhost:8081/swagger-ui.html
- API Docs JSON: http://localhost:8081/api-docs

## 🔍 Health Check

Check application health:
```
GET http://localhost:8081/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## 🗄️ Database Schema

The database consists of 24 tables:

### Core Tables
- `users` - User accounts
- `admin_users` - Admin accounts
- `addresses` - User addresses
- `categories` - Product categories
- `products` - Product catalog
- `product_images` - Product images
- `inventory` - Product inventory

### Transaction Tables
- `carts` - Shopping carts
- `cart_items` - Cart items
- `orders` - Customer orders
- `order_items` - Order line items
- `payments` - Payment transactions
- `shipments` - Shipment tracking

### Supporting Tables
- `reviews` - Product reviews
- `discount_codes` - Promotional codes
- `notifications` - Notification queue
- `otp_verifications` - OTP codes
- `user_sessions` - User sessions
- `audit_logs` - Audit trail
- And more...

See [DATABASE_CHECKLIST.md](DATABASE_CHECKLIST.md) for complete schema verification.

## 🚢 Deployment

### Supabase + Any Cloud Platform

1. Set up Supabase database (see [SUPABASE_SETUP.md](SUPABASE_SETUP.md))
2. Configure environment variables in your hosting platform
3. Build the JAR file: `mvn clean package -DskipTests`
4. Deploy the JAR file to your hosting platform

### Recommended Hosting Platforms
- Railway
- Render
- Heroku
- AWS Elastic Beanstalk
- Google Cloud Run
- Azure App Service

### Environment Variables for Production

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://db.[project-ref].supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=your_64char_secret
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
FRONTEND_URL=https://your-frontend-domain.com
HIBERNATE_DDL_AUTO=validate
API_DOCS_ENABLED=false
SWAGGER_UI_ENABLED=false
```

## 🐛 Troubleshooting

### Database Connection Errors
- Verify Supabase project is active (not paused)
- Check connection string format
- Ensure SSL mode is enabled (`?sslmode=require`)

### Authentication Issues
- Verify JWT secret is set and matches across restarts
- Check token expiration times
- Verify user credentials

### Email Not Sending
- Verify SMTP credentials
- For Gmail, use App Password, not regular password
- Check firewall/network restrictions

## 📝 Environment Profiles

- **dev** - Development (auto-creates tables, verbose logging)
- **prod** - Production (validates schema, minimal logging)
- **test** - Testing (in-memory database)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📧 Contact

For questions or support, please contact: [your-email@example.com]

## 🔄 Changelog

### Version 1.0.0 (Current)
- Initial release
- Complete e-commerce functionality
- User authentication and authorization
- Product management
- Order processing
- Payment integration ready
- Admin panel support
- Multi-language support
- Audit logging

## 🎯 Roadmap

- [ ] Add webhook support for payment gateways
- [ ] Implement real-time notifications with WebSocket
- [ ] Add product recommendations engine
- [ ] Implement advanced search with Elasticsearch
- [ ] Add caching with Redis
- [ ] Implement rate limiting
- [ ] Add GraphQL API support
- [ ] Enhanced analytics and reporting

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Supabase Documentation](https://supabase.com/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)

---

**Built with ❤️ using Spring Boot**
