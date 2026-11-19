using System.Security.Cryptography;
using System.Text;
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

app.Run();

internal sealed record LoginRequest(string Email, string Password);
internal sealed record LoginResponse(int UserId, string Email, string FullName, string Role);
internal sealed record ErrorResponse(string Message);
internal sealed record DatabaseHealthResponse(string Status, string? Database);

internal static class DemoPasswordHasher
{
    private const int SaltSize = 16;
    private const int HashSize = 32;

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
