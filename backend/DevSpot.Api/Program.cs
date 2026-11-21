using System.Security.Cryptography;
using System.Text;
using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Data.SqlClient;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddRateLimiter(options =>
{
    options.AddFixedWindowLimiter("login", limiter =>
    {
        limiter.PermitLimit = 5;
        limiter.Window = TimeSpan.FromMinutes(1);
        limiter.QueueLimit = 0;
    });
    options.AddFixedWindowLimiter("register", limiter =>
    {
        limiter.PermitLimit = 3;
        limiter.Window = TimeSpan.FromMinutes(1);
        limiter.QueueLimit = 0;
    });
});

var app = builder.Build();
app.UseRateLimiter();

app.MapGet("/api/health/db", async (IConfiguration configuration, CancellationToken cancellationToken) =>
{
    var connectionString = configuration.GetConnectionString("DevSpot");
    if (string.IsNullOrWhiteSpace(connectionString))
    {
        return Results.Problem(
            title: "SQL Server connection is not configured.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }

    try
    {
        await using var connection = new SqlConnection(connectionString);
        await connection.OpenAsync(cancellationToken);
        await using var command = new SqlCommand("SELECT DB_NAME()", connection);
        var databaseName = (string?)await command.ExecuteScalarAsync(cancellationToken);

        return Results.Ok(new DatabaseHealthResponse("connected", databaseName));
    }
    catch (SqlException)
    {
        return Results.Problem(
            title: "Could not connect to SQL Server. Check the API connection settings and SQL Server service.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }
})
.WithName("CheckDatabaseConnection");

app.MapPost("/api/auth/login", async (
    LoginRequest request,
    IConfiguration configuration,
    ILogger<Program> logger,
    CancellationToken cancellationToken) =>
{
    if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrEmpty(request.Password))
    {
        return Results.BadRequest(new ErrorResponse("Email and password are required."));
    }

    var connectionString = configuration.GetConnectionString("DevSpot");
    if (string.IsNullOrWhiteSpace(connectionString))
    {
        return Results.Problem(
            title: "SQL Server connection is not configured.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }

    try
    {
        await using var connection = new SqlConnection(connectionString);
        await connection.OpenAsync(cancellationToken);
        await using var command = new SqlCommand(
            """
            SELECT UserId, Email, FullName, Role, CONVERT(VARCHAR(256), PasswordHash)
            FROM dbo.Users
            WHERE Email = @Email AND IsActive = 1
            """,
            connection);
        command.Parameters.Add("@Email", System.Data.SqlDbType.NVarChar, 254).Value = request.Email.Trim();

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (!await reader.ReadAsync(cancellationToken))
        {
            return Results.Unauthorized();
        }

        var passwordHash = reader.IsDBNull(4) ? null : reader.GetString(4);
        if (passwordHash is null || !DemoPasswordHasher.Verify(request.Password, passwordHash))
        {
            return Results.Unauthorized();
        }

        return Results.Ok(new LoginResponse(
            reader.GetInt32(0),
            reader.GetString(1),
            reader.GetString(2),
            reader.GetString(3)));
    }
    catch (SqlException exception)
    {
        logger.LogError(exception, "SQL Server login query failed.");
        return Results.Problem(
            title: "Could not reach the DevSpot database.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }
})
.RequireRateLimiting("login")
.WithName("Login");

app.MapPost("/api/auth/register", async (
    RegisterRequest request,
    IConfiguration configuration,
    ILogger<Program> logger,
    CancellationToken cancellationToken) =>
{
    var email = request.Email?.Trim().ToLowerInvariant();
    var password = request.Password;
    var fullName = request.FullName?.Trim();
    var role = request.Role?.Trim();
    var companyName = request.CompanyName?.Trim();
    var githubUrl = request.GithubUrl?.Trim();

    if (string.IsNullOrWhiteSpace(email) ||
        email.Length > 254 ||
        !new EmailAddressAttribute().IsValid(email) ||
        string.IsNullOrWhiteSpace(password) ||
        password.Length is < 8 or > 128 ||
        string.IsNullOrWhiteSpace(fullName) ||
        fullName.Length > 120 ||
        role is not ("Creator" or "Recruiter"))
    {
        return Results.BadRequest(new ErrorResponse("Registration details are invalid."));
    }

    if (role == "Creator" && githubUrl?.Length > 500)
    {
        return Results.BadRequest(new ErrorResponse("The profile URL is too long."));
    }

    if (role == "Recruiter" &&
        (string.IsNullOrWhiteSpace(companyName) || companyName.Length > 150))
    {
        return Results.BadRequest(new ErrorResponse("A valid company name is required."));
    }

    var connectionString = configuration.GetConnectionString("DevSpot");
    if (string.IsNullOrWhiteSpace(connectionString))
    {
        return Results.Problem(
            title: "SQL Server connection is not configured.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }

    try
    {
        await using var connection = new SqlConnection(connectionString);
        await connection.OpenAsync(cancellationToken);
        await using var transaction = connection.BeginTransaction();

        var passwordHash = Encoding.ASCII.GetBytes(DemoPasswordHasher.Hash(password));
        int userId;
        await using (var userCommand = new SqlCommand(
            """
            INSERT INTO dbo.Users (Email, FullName, Role, PasswordHash)
            OUTPUT INSERTED.UserId
            VALUES (@Email, @FullName, @Role, @PasswordHash)
            """,
            connection,
            transaction))
        {
            userCommand.Parameters.Add("@Email", System.Data.SqlDbType.NVarChar, 254).Value = email;
            userCommand.Parameters.Add("@FullName", System.Data.SqlDbType.NVarChar, 120).Value = fullName;
            userCommand.Parameters.Add("@Role", System.Data.SqlDbType.VarChar, 20).Value = role;
            userCommand.Parameters.Add("@PasswordHash", System.Data.SqlDbType.VarBinary, 256).Value = passwordHash;
            userId = (int)(await userCommand.ExecuteScalarAsync(cancellationToken)
                ?? throw new InvalidOperationException("The created account did not return a user ID."));
        }

        if (role == "Creator")
        {
            await using var profileCommand = new SqlCommand(
                """
                INSERT INTO dbo.CreatorProfiles (UserId, Username, GithubUrl)
                VALUES (@UserId, @Username, @GithubUrl)
                """,
                connection,
                transaction);
            profileCommand.Parameters.Add("@UserId", System.Data.SqlDbType.Int).Value = userId;
            profileCommand.Parameters.Add("@Username", System.Data.SqlDbType.NVarChar, 50).Value = $"creator_{userId}";
            profileCommand.Parameters.Add("@GithubUrl", System.Data.SqlDbType.NVarChar, 500).Value =
                string.IsNullOrWhiteSpace(githubUrl) ? DBNull.Value : githubUrl;
            await profileCommand.ExecuteNonQueryAsync(cancellationToken);
        }
        else
        {
            int companyId;
            await using (var companyCommand = new SqlCommand(
                "SELECT CompanyId FROM dbo.Companies WHERE CompanyName = @CompanyName",
                connection,
                transaction))
            {
                companyCommand.Parameters.Add("@CompanyName", System.Data.SqlDbType.NVarChar, 150).Value = companyName!;
                var existingCompanyId = await companyCommand.ExecuteScalarAsync(cancellationToken);
                if (existingCompanyId is int existingId)
                {
                    companyId = existingId;
                }
                else
                {
                    await using var insertCompanyCommand = new SqlCommand(
                        """
                        INSERT INTO dbo.Companies (CompanyName)
                        OUTPUT INSERTED.CompanyId
                        VALUES (@CompanyName)
                        """,
                        connection,
                        transaction);
                    insertCompanyCommand.Parameters.Add("@CompanyName", System.Data.SqlDbType.NVarChar, 150).Value = companyName!;
                    companyId = (int)(await insertCompanyCommand.ExecuteScalarAsync(cancellationToken)
                        ?? throw new InvalidOperationException("The created company did not return an ID."));
                }
            }

            await using var recruiterCommand = new SqlCommand(
                """
                INSERT INTO dbo.RecruiterProfiles (UserId, CompanyId, Position)
                VALUES (@UserId, @CompanyId, @Position)
                """,
                connection,
                transaction);
            recruiterCommand.Parameters.Add("@UserId", System.Data.SqlDbType.Int).Value = userId;
            recruiterCommand.Parameters.Add("@CompanyId", System.Data.SqlDbType.Int).Value = companyId;
            recruiterCommand.Parameters.Add("@Position", System.Data.SqlDbType.NVarChar, 100).Value = "Recruiter";
            await recruiterCommand.ExecuteNonQueryAsync(cancellationToken);
        }

        await transaction.CommitAsync(cancellationToken);
        return Results.Ok(new LoginResponse(userId, email, fullName, role));
    }
    catch (SqlException exception) when (exception.Number is 2601 or 2627)
    {
        return Results.Conflict(new ErrorResponse(
            "The email or company name already exists. Verify the details and try again."));
    }
    catch (SqlException exception)
    {
        logger.LogError(exception, "SQL Server registration transaction failed.");
        return Results.Problem(
            title: "Could not create the account in the DevSpot database.",
            statusCode: StatusCodes.Status503ServiceUnavailable);
    }
})
.RequireRateLimiting("register")
.WithName("Register");

app.Run();

internal sealed record LoginRequest(string Email, string Password);
internal sealed record RegisterRequest(
    string? Email,
    string? Password,
    string? FullName,
    string? Role,
    string? GithubUrl,
    string? CompanyName);
internal sealed record LoginResponse(int UserId, string Email, string FullName, string Role);
internal sealed record ErrorResponse(string Message);
internal sealed record DatabaseHealthResponse(string Status, string? Database);

internal static class DemoPasswordHasher
{
    private const int Iterations = 600_000;
    private const int SaltSize = 16;
    private const int HashSize = 32;

    public static string Hash(string password)
    {
        var salt = RandomNumberGenerator.GetBytes(SaltSize);
        var hash = Rfc2898DeriveBytes.Pbkdf2(
            Encoding.UTF8.GetBytes(password),
            salt,
            Iterations,
            HashAlgorithmName.SHA256,
            HashSize);
        return $"pbkdf2_sha256${Iterations}${Convert.ToBase64String(salt)}${Convert.ToBase64String(hash)}";
    }

    public static bool Verify(string password, string encodedHash)
    {
        var parts = encodedHash.Split('$');
        if (parts.Length != 4 ||
            parts[0] != "pbkdf2_sha256" ||
            !int.TryParse(parts[1], out var iterations) ||
            iterations is < 100_000 or > 1_000_000)
        {
            return false;
        }

        try
        {
            var salt = Convert.FromBase64String(parts[2]);
            var expectedHash = Convert.FromBase64String(parts[3]);
            if (salt.Length != SaltSize || expectedHash.Length != HashSize)
            {
                return false;
            }

            var actualHash = Rfc2898DeriveBytes.Pbkdf2(
                Encoding.UTF8.GetBytes(password),
                salt,
                iterations,
                HashAlgorithmName.SHA256,
                HashSize);

            return CryptographicOperations.FixedTimeEquals(actualHash, expectedHash);
        }
        catch (FormatException)
        {
            return false;
        }
    }
}
