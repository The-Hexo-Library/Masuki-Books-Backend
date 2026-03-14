# Supabase Setup Guide for Masukibooks Backend

## Prerequisites
- Supabase account (https://supabase.com)
- Java 21 installed
- Maven 3.8+ installed

## Step 1: Create Supabase Project

1. Go to https://supabase.com and sign in
2. Click "New Project"
3. Choose your organization
4. Enter project details:
   - Name: masukibooks (or your preferred name)
   - Database Password: Choose a strong password (save this!)
   - Region: Choose closest to your users
5. Wait for project to be created (2-3 minutes)

## Step 2: Get Database Credentials

1. In your Supabase dashboard, go to **Settings** → **Database**
2. Find the "Connection String" section
3. Select **JDBC** tab
4. Copy the connection string (format: `jdbc:postgresql://db.[project-ref].supabase.co:5432/postgres`)
5. Note down:
   - Host: `db.[project-ref].supabase.co`
   - Port: `5432`
   - Database: `postgres`
   - User: `postgres`
   - Password: (the one you set during project creation)

## Step 3: Enable Required Extensions

Run these commands in Supabase SQL Editor (Dashboard → SQL Editor):

```sql
-- Enable UUID extension (should already be enabled)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

## Step 4: Run Database Schema

1. Go to Supabase Dashboard → SQL Editor
2. Click "New Query"
3. Copy and paste the entire content from the SQL script provided
4. Click "Run" to execute
5. Verify all tables are created in Database → Tables

## Step 5: Configure Application

### For Local Development with Supabase:

Create a `.env` file or set these in `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.[YOUR-PROJECT-REF].supabase.co:5432/postgres
    username: postgres
    password: YOUR_SUPABASE_PASSWORD
```

### For Production Deployment:

Set environment variables in your hosting platform:

```bash
DATABASE_URL=jdbc:postgresql://db.[YOUR-PROJECT-REF].supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_supabase_password
JWT_SECRET=generate_secure_random_64char_string
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
FRONTEND_URL=https://your-frontend-domain.com
SPRING_PROFILES_ACTIVE=prod
```

## Step 6: Configure Row Level Security (Optional but Recommended)

If you want to use Supabase Auth with RLS:

```sql
-- Enable RLS on tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
-- Add similar for other tables

-- Create policies as needed
CREATE POLICY "Users can view their own data"
  ON users FOR SELECT
  USING (auth.uid()::uuid = user_id);
```

## Step 7: Test Connection

Run the application:

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or using Java
java -jar target/masukibooks-backend-1.0.0.jar --spring.profiles.active=dev
```

Check logs for successful connection:
```
HikariPool-1 - Start completed.
```

## Step 8: Verify Database Connection

1. Access Swagger UI: http://localhost:8081/swagger-ui.html
2. Test health endpoint: http://localhost:8081/actuator/health
3. Should return: `{"status":"UP"}`

## Connection String Examples

### Direct Connection (for configuration):
```
jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres
```

### With SSL (recommended for production):
```
jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?sslmode=require
```

### Connection Pooling Enabled:
```
jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?sslmode=require&prepareThreshold=0
```

## Important Notes

1. **Supabase Pooler**: For applications with many connections, use Supabase's connection pooler:
   - Transaction mode: `db.[project-ref].supabase.co:6543/postgres`
   - Session mode: `db.[project-ref].supabase.co:5432/postgres`

2. **SSL Connection**: Supabase requires SSL. Add `?sslmode=require` to connection string if needed.

3. **Connection Limits**:
   - Free tier: 60 connections
   - Pro tier: 200+ connections
   - Use connection pooling (already configured in application.yml)

4. **Firewall**: Supabase allows connections from all IPs by default. Configure in Settings → Database → Connection Pooling

5. **Backups**: Supabase automatically backs up your database daily on Pro tier

## Troubleshooting

### Connection timeout:
- Check if Supabase project is running (not paused)
- Verify connection string format
- Check firewall rules

### Authentication failed:
- Verify password is correct
- Check username (should be `postgres`)
- Ensure database is not paused (free tier pauses after 7 days of inactivity)

### SSL error:
Add to datasource URL: `?sslmode=require`

### Too many connections:
- Reduce `maximum-pool-size` in application.yml
- Use Supabase connection pooler (port 6543)

## Security Recommendations

1. **Change JWT Secret**: Generate a secure random string (min 64 characters)
   ```bash
   openssl rand -hex 64
   ```

2. **Use Environment Variables**: Never hardcode sensitive data

3. **Enable SSL**: Always use SSL for production connections

4. **Rotate Passwords**: Regularly update database passwords

5. **Monitor Connections**: Check Supabase dashboard for connection usage

6. **Backup Strategy**: Enable point-in-time recovery on Pro tier

## Next Steps

1. Create an admin user manually or via seed script
2. Test API endpoints
3. Configure email service for OTP
4. Set up payment gateway integration
5. Configure frontend CORS settings
6. Deploy to your hosting platform (Heroku, Railway, AWS, etc.)

## Support

- Supabase Docs: https://supabase.com/docs
- Supabase Discord: https://discord.supabase.com
- Spring Boot Docs: https://spring.io/guides
