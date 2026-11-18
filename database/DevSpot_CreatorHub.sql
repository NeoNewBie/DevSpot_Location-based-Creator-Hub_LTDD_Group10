/*
  DevSpot Creator Hub - SQL Server schema and demo data

  Run this script in SQL Server Management Studio or Azure Data Studio.
  It creates a separate DevSpotCreatorHub database and can be run repeatedly.
  Existing rows are not dropped or overwritten.
  All demo accounts use password: DevSpotDemo@2026!
  PBKDF2-SHA256 hashes are stored as ASCII bytes; this script does not enable app authentication.
*/

IF DB_ID(N'DevSpotCreatorHub') IS NULL
    CREATE DATABASE DevSpotCreatorHub;
GO

USE DevSpotCreatorHub;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserId       INT IDENTITY(1,1) PRIMARY KEY,
        Email        NVARCHAR(254) NOT NULL UNIQUE,
        FullName     NVARCHAR(120) NOT NULL,
        Role         VARCHAR(20) NOT NULL
                     CONSTRAINT CK_Users_Role CHECK (Role IN ('Creator', 'Recruiter', 'Admin')),
        AvatarUrl    NVARCHAR(500) NULL,
        PasswordHash VARBINARY(256) NULL,
        IsActive     BIT NOT NULL CONSTRAINT DF_Users_IsActive DEFAULT 1,
        CreatedAt    DATETIME2 NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.CreatorProfiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CreatorProfiles (
        UserId          INT PRIMARY KEY REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        Username        NVARCHAR(50) NOT NULL UNIQUE,
        Headline        NVARCHAR(150) NULL,
        Bio             NVARCHAR(1000) NULL,
        School          NVARCHAR(150) NULL,
        Major           NVARCHAR(120) NULL,
        Location        NVARCHAR(150) NULL,
        Latitude        DECIMAL(9,6) NULL,
        Longitude       DECIMAL(9,6) NULL,
        GithubUrl       NVARCHAR(500) NULL,
        PortfolioUrl    NVARCHAR(500) NULL,
        OpenToWork      BIT NOT NULL CONSTRAINT DF_CreatorProfiles_OpenToWork DEFAULT 1,
        LocationPrivacy VARCHAR(20) NOT NULL
                        CONSTRAINT DF_CreatorProfiles_LocationPrivacy DEFAULT 'Area'
                        CONSTRAINT CK_CreatorProfiles_LocationPrivacy
                            CHECK (LocationPrivacy IN ('Exact', 'Area', 'Hidden')),
        CONSTRAINT CK_CreatorProfiles_Latitude CHECK (Latitude IS NULL OR Latitude BETWEEN -90 AND 90),
        CONSTRAINT CK_CreatorProfiles_Longitude CHECK (Longitude IS NULL OR Longitude BETWEEN -180 AND 180)
    );
END;

IF OBJECT_ID(N'dbo.Companies', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Companies (
        CompanyId   INT IDENTITY(1,1) PRIMARY KEY,
        CompanyName NVARCHAR(150) NOT NULL UNIQUE,
        Description NVARCHAR(2000) NULL,
        Website     NVARCHAR(500) NULL,
        LogoUrl     NVARCHAR(500) NULL,
        Address     NVARCHAR(300) NULL,
        Latitude    DECIMAL(9,6) NULL,
        Longitude   DECIMAL(9,6) NULL,
        CompanySize NVARCHAR(30) NULL,
        CreatedAt   DATETIME2 NOT NULL CONSTRAINT DF_Companies_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_Companies_Latitude CHECK (Latitude IS NULL OR Latitude BETWEEN -90 AND 90),
        CONSTRAINT CK_Companies_Longitude CHECK (Longitude IS NULL OR Longitude BETWEEN -180 AND 180)
    );
END;

IF OBJECT_ID(N'dbo.RecruiterProfiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RecruiterProfiles (
        UserId    INT PRIMARY KEY REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        CompanyId INT NOT NULL REFERENCES dbo.Companies(CompanyId),
        Position  NVARCHAR(100) NULL
    );
END;

IF OBJECT_ID(N'dbo.Projects', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Projects (
        ProjectId       INT IDENTITY(1,1) PRIMARY KEY,
        CreatorId       INT NOT NULL REFERENCES dbo.CreatorProfiles(UserId),
        Title           NVARCHAR(200) NOT NULL,
        Tagline         NVARCHAR(128) NULL,
        Description     NVARCHAR(MAX) NULL,
        RepositoryUrl   NVARCHAR(500) NULL,
        DemoUrl         NVARCHAR(500) NULL,
        CoverImageUrl   NVARCHAR(500) NULL,
        Location        NVARCHAR(150) NULL,
        Latitude        DECIMAL(9,6) NULL,
        Longitude       DECIMAL(9,6) NULL,
        LocationPrivacy VARCHAR(20) NOT NULL
                        CONSTRAINT DF_Projects_LocationPrivacy DEFAULT 'Area'
                        CONSTRAINT CK_Projects_LocationPrivacy
                            CHECK (LocationPrivacy IN ('Exact', 'Area', 'Hidden')),
        Status          VARCHAR(20) NOT NULL
                        CONSTRAINT DF_Projects_Status DEFAULT 'Published'
                        CONSTRAINT CK_Projects_Status CHECK (Status IN ('Draft', 'Published', 'Archived')),
        CreatedAt       DATETIME2 NOT NULL CONSTRAINT DF_Projects_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_Projects_Creator_Title UNIQUE (CreatorId, Title),
        CONSTRAINT CK_Projects_Latitude CHECK (Latitude IS NULL OR Latitude BETWEEN -90 AND 90),
        CONSTRAINT CK_Projects_Longitude CHECK (Longitude IS NULL OR Longitude BETWEEN -180 AND 180)
    );
END;

IF OBJECT_ID(N'dbo.Skills', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Skills (
        SkillId   INT IDENTITY(1,1) PRIMARY KEY,
        SkillName NVARCHAR(60) NOT NULL UNIQUE
    );
END;

IF OBJECT_ID(N'dbo.CreatorSkills', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CreatorSkills (
        CreatorId INT NOT NULL REFERENCES dbo.CreatorProfiles(UserId) ON DELETE CASCADE,
        SkillId   INT NOT NULL REFERENCES dbo.Skills(SkillId),
        Level     TINYINT NOT NULL
                  CONSTRAINT DF_CreatorSkills_Level DEFAULT 3
                  CONSTRAINT CK_CreatorSkills_Level CHECK (Level BETWEEN 1 AND 5),
        CONSTRAINT PK_CreatorSkills PRIMARY KEY (CreatorId, SkillId)
    );
END;

IF OBJECT_ID(N'dbo.ProjectSkills', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProjectSkills (
        ProjectId INT NOT NULL REFERENCES dbo.Projects(ProjectId) ON DELETE CASCADE,
        SkillId   INT NOT NULL REFERENCES dbo.Skills(SkillId),
        CONSTRAINT PK_ProjectSkills PRIMARY KEY (ProjectId, SkillId)
    );
END;

IF OBJECT_ID(N'dbo.Follows', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Follows (
        FollowerId INT NOT NULL REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        FolloweeId INT NOT NULL REFERENCES dbo.Users(UserId),
        CreatedAt  DATETIME2 NOT NULL CONSTRAINT DF_Follows_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_Follows PRIMARY KEY (FollowerId, FolloweeId),
        CONSTRAINT CK_Follows_DifferentUsers CHECK (FollowerId <> FolloweeId)
    );
END;

IF OBJECT_ID(N'dbo.ProjectLikes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProjectLikes (
        ProjectId INT NOT NULL REFERENCES dbo.Projects(ProjectId) ON DELETE CASCADE,
        UserId    INT NOT NULL REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ProjectLikes_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_ProjectLikes PRIMARY KEY (ProjectId, UserId)
    );
END;

IF OBJECT_ID(N'dbo.ProjectBookmarks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProjectBookmarks (
        ProjectId INT NOT NULL REFERENCES dbo.Projects(ProjectId) ON DELETE CASCADE,
        UserId    INT NOT NULL REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ProjectBookmarks_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_ProjectBookmarks PRIMARY KEY (ProjectId, UserId)
    );
END;

IF OBJECT_ID(N'dbo.ProjectComments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProjectComments (
        CommentId INT IDENTITY(1,1) PRIMARY KEY,
        ProjectId INT NOT NULL REFERENCES dbo.Projects(ProjectId) ON DELETE CASCADE,
        UserId    INT NOT NULL REFERENCES dbo.Users(UserId),
        Content   NVARCHAR(2000) NOT NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ProjectComments_CreatedAt DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.Messages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Messages (
        MessageId  BIGINT IDENTITY(1,1) PRIMARY KEY,
        SenderId   INT NOT NULL REFERENCES dbo.Users(UserId),
        ReceiverId INT NOT NULL REFERENCES dbo.Users(UserId),
        Content    NVARCHAR(2000) NOT NULL,
        IsRead     BIT NOT NULL CONSTRAINT DF_Messages_IsRead DEFAULT 0,
        SentAt     DATETIME2 NOT NULL CONSTRAINT DF_Messages_SentAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_Messages_DifferentUsers CHECK (SenderId <> ReceiverId)
    );
END;

IF OBJECT_ID(N'dbo.InterviewInvitations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InterviewInvitations (
        InvitationId INT IDENTITY(1,1) PRIMARY KEY,
        RecruiterId  INT NOT NULL REFERENCES dbo.RecruiterProfiles(UserId),
        CreatorId    INT NOT NULL REFERENCES dbo.CreatorProfiles(UserId),
        ProjectId    INT NULL REFERENCES dbo.Projects(ProjectId),
        ScheduledAt  DATETIME2 NULL,
        Location     NVARCHAR(300) NULL,
        SalaryOffer  NVARCHAR(100) NULL,
        Note         NVARCHAR(2000) NULL,
        Status       VARCHAR(20) NOT NULL
                     CONSTRAINT DF_InterviewInvitations_Status DEFAULT 'Pending'
                     CONSTRAINT CK_InterviewInvitations_Status
                         CHECK (Status IN ('Pending', 'Accepted', 'Declined', 'Proposed', 'Cancelled')),
        CreatedAt    DATETIME2 NOT NULL CONSTRAINT DF_InterviewInvitations_CreatedAt DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.ProjectReviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProjectReviews (
        ReviewId   INT IDENTITY(1,1) PRIMARY KEY,
        ProjectId  INT NOT NULL REFERENCES dbo.Projects(ProjectId) ON DELETE CASCADE,
        ReviewerId INT NOT NULL REFERENCES dbo.Users(UserId),
        Rating     TINYINT NOT NULL CONSTRAINT CK_ProjectReviews_Rating CHECK (Rating BETWEEN 1 AND 5),
        Content    NVARCHAR(2000) NULL,
        CreatedAt  DATETIME2 NOT NULL CONSTRAINT DF_ProjectReviews_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_ProjectReviews_Project_Reviewer UNIQUE (ProjectId, ReviewerId)
    );
END;

IF OBJECT_ID(N'dbo.Notifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications (
        NotificationId INT IDENTITY(1,1) PRIMARY KEY,
        UserId          INT NOT NULL REFERENCES dbo.Users(UserId) ON DELETE CASCADE,
        Title           NVARCHAR(150) NOT NULL,
        Body            NVARCHAR(500) NULL,
        Type            VARCHAR(30) NULL,
        ReferenceId     INT NULL,
        IsRead          BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT 0,
        CreatedAt       DATETIME2 NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Projects') AND name = N'IX_Projects_Status_CreatedAt')
    CREATE INDEX IX_Projects_Status_CreatedAt ON dbo.Projects(Status, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Projects') AND name = N'IX_Projects_Location')
    CREATE INDEX IX_Projects_Location ON dbo.Projects(Latitude, Longitude) WHERE Latitude IS NOT NULL AND Longitude IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.ProjectComments') AND name = N'IX_ProjectComments_Project_CreatedAt')
    CREATE INDEX IX_ProjectComments_Project_CreatedAt ON dbo.ProjectComments(ProjectId, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Messages') AND name = N'IX_Messages_Receiver_SentAt')
    CREATE INDEX IX_Messages_Receiver_SentAt ON dbo.Messages(ReceiverId, SentAt DESC);
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

INSERT INTO dbo.Users (Email, FullName, Role)
SELECT seed.Email, seed.FullName, seed.Role
FROM (VALUES
    (N'quoc.le@devspot.demo', N'Lê Anh Quốc', 'Creator'),
    (N'tuan.kiet@devspot.demo', N'Nguyễn Tuấn Kiệt', 'Creator'),
    (N'mai.lan@devspot.demo', N'Trần Mai Lan', 'Creator'),
    (N'minh.tran@nexusai.demo', N'Trần Minh', 'Recruiter'),
    (N'anh.nguyen@devspot.demo', N'Nguyễn Hoàng Anh', 'Creator')
) AS seed(Email, FullName, Role)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Users u WHERE u.Email = seed.Email);

UPDATE u
SET PasswordHash = CONVERT(VARBINARY(256), seed.EncodedHash)
FROM dbo.Users u
JOIN (VALUES
    (N'quoc.le@devspot.demo', 'pbkdf2_sha256$600000$taOGWQtigNxFJD7uqU5I5g==$4FxC//nXqhWKqoN0JAid/m0+KBf+vDx0yFDHEMDFNqU='),
    (N'tuan.kiet@devspot.demo', 'pbkdf2_sha256$600000$CKDGmZBbVM5KDy0x54J/gA==$lDR3F3AnG+pufyY2h+xU+1zddyY/o92quBpWP6M4HYs='),
    (N'mai.lan@devspot.demo', 'pbkdf2_sha256$600000$TOmpq1GAI9Wt/UGcRI5LiA==$zHrOfSJ0D8o5shDEHu4RphCBRCMjEJU29GGQ2Xbwa6Q='),
    (N'minh.tran@nexusai.demo', 'pbkdf2_sha256$600000$gkq2YbsuuA7VMh7K+CWIRA==$jBjzSsCLjLCHBM9ruRJWjZzdXRsXlR7iDQsArpQi0e0='),
    (N'anh.nguyen@devspot.demo', 'pbkdf2_sha256$600000$Nonrabm5tNchGwdXpi/3eg==$4gKUq16nB1uc1r34DG/k0SacoiWKTSI0QykN/pTQRPs=')
) AS seed(Email, EncodedHash) ON seed.Email = u.Email
WHERE u.PasswordHash IS NULL;

INSERT INTO dbo.CreatorProfiles
    (UserId, Username, Headline, Bio, School, Major, Location, Latitude, Longitude, GithubUrl, OpenToWork, LocationPrivacy)
SELECT u.UserId, seed.Username, seed.Headline, seed.Bio, seed.School, seed.Major,
       seed.Location, seed.Latitude, seed.Longitude, seed.GithubUrl, seed.OpenToWork, seed.LocationPrivacy
FROM (VALUES
    (N'quoc.le@devspot.demo', N'QuocDev', N'Full-stack & Mobile Developer',
     N'Xây dựng hệ sinh thái bản đồ vị trí cho sinh viên IT. Đang phát triển ứng dụng DevSpot.',
     N'ĐH Sư phạm Kỹ thuật Đà Nẵng', N'Công nghệ thông tin', N'Đà Nẵng', 16.075900, 108.153400,
     N'https://github.com/quocdev', 1, 'Area'),
    (N'tuan.kiet@devspot.demo', N'TuanKiet', N'IoT & Edge AI Developer',
     N'Thích kết hợp thiết bị IoT, machine learning và các sản phẩm phục vụ cộng đồng.',
     N'ĐH Bách khoa Đà Nẵng', N'Kỹ thuật máy tính', N'Đà Nẵng', 16.073200, 108.149800,
     N'https://github.com/tuan-kiet', 1, 'Area'),
    (N'mai.lan@devspot.demo', N'MaiLan', N'Android Developer',
     N'Phát triển ứng dụng di động và giải pháp số cho khuôn viên đại học.',
     N'ĐH Sư phạm Kỹ thuật Đà Nẵng', N'Công nghệ phần mềm', N'Đà Nẵng', 16.078100, 108.155200,
     N'https://github.com/mai-lan', 1, 'Area'),
    (N'anh.nguyen@devspot.demo', N'HoangAnh', N'Backend Developer',
     N'Quan tâm đến API, dữ liệu không gian và hệ thống có khả năng mở rộng.',
     N'ĐH Bách khoa Đà Nẵng', N'Công nghệ thông tin', N'Đà Nẵng', 16.047100, 108.220800,
     N'https://github.com/hoanganh', 1, 'Area')
) AS seed(Email, Username, Headline, Bio, School, Major, Location, Latitude, Longitude, GithubUrl, OpenToWork, LocationPrivacy)
JOIN dbo.Users u ON u.Email = seed.Email
WHERE NOT EXISTS (SELECT 1 FROM dbo.CreatorProfiles p WHERE p.UserId = u.UserId);

INSERT INTO dbo.Companies (CompanyName, Description, Website, Address, Latitude, Longitude, CompanySize)
SELECT seed.CompanyName, seed.Description, seed.Website, seed.Address, seed.Latitude, seed.Longitude, seed.CompanySize
FROM (VALUES
    (N'Nexus AI Labs', N'Phòng nghiên cứu và phát triển AI, IoT tại Đà Nẵng.',
     N'https://example.com/nexus-ai', N'Khu Công Nghệ Cao, Đà Nẵng', 15.999500, 108.259100, N'50-100'),
    (N'DevSpot Community', N'Cộng đồng sinh viên và nhà phát triển địa phương.',
     N'https://example.com/devspot', N'Hải Châu, Đà Nẵng', 16.061200, 108.219900, N'1-10')
) AS seed(CompanyName, Description, Website, Address, Latitude, Longitude, CompanySize)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Companies c WHERE c.CompanyName = seed.CompanyName);

INSERT INTO dbo.RecruiterProfiles (UserId, CompanyId, Position)
SELECT u.UserId, c.CompanyId, N'Talent Partner'
FROM dbo.Users u
JOIN dbo.Companies c ON c.CompanyName = N'Nexus AI Labs'
WHERE u.Email = N'minh.tran@nexusai.demo'
  AND NOT EXISTS (SELECT 1 FROM dbo.RecruiterProfiles rp WHERE rp.UserId = u.UserId);

INSERT INTO dbo.Skills (SkillName)
SELECT seed.SkillName
FROM (VALUES
    (N'Kotlin'), (N'Android'), (N'ASP.NET Core'), (N'SQL Server'), (N'OSMDroid'),
    (N'Python'), (N'TensorFlow Lite'), (N'MQTT'), (N'C++'), (N'IoT'),
    (N'Java'), (N'Figma'), (N'Firebase'), (N'React')
) AS seed(SkillName)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Skills s WHERE s.SkillName = seed.SkillName);

INSERT INTO dbo.Projects
    (CreatorId, Title, Tagline, Description, RepositoryUrl, DemoUrl, Location, Latitude, Longitude, LocationPrivacy)
SELECT cp.UserId, seed.Title, seed.Tagline, seed.Description, seed.RepositoryUrl, seed.DemoUrl,
       seed.Location, seed.Latitude, seed.Longitude, seed.LocationPrivacy
FROM (VALUES
    (N'quoc.le@devspot.demo', N'DevSpot Location-Based Hub',
     N'Kết nối Creator và nhà tuyển dụng bằng bản đồ dự án.',
     N'Nền tảng portfolio theo vị trí, cho phép khám phá dự án và Creator quanh các trường đại học.',
     N'https://github.com/quocdev/devspot', N'https://example.com/devspot-demo',
     N'UTE Đà Nẵng', 16.075900, 108.153400, 'Area'),
    (N'tuan.kiet@devspot.demo', N'Cybersafe IoT / Edge AI',
     N'Phát hiện bất thường ngay trên thiết bị biên.',
     N'Mô hình Edge AI giám sát cảm biến IoT, tối ưu khả năng phản hồi và giảm dữ liệu gửi lên cloud.',
     N'https://github.com/tuan-kiet/cybersafe-iot', N'https://example.com/cybersafe-demo',
     N'ĐH Bách khoa Đà Nẵng', 16.073200, 108.149800, 'Area'),
    (N'mai.lan@devspot.demo', N'Smart Campus Monitor',
     N'Theo dõi môi trường khuôn viên theo thời gian thực.',
     N'Ứng dụng Android hiển thị dữ liệu cảm biến MQTT và cảnh báo chất lượng không khí.',
     N'https://github.com/mai-lan/smart-campus', N'https://example.com/campus-demo',
     N'UTE Đà Nẵng', 16.078100, 108.155200, 'Area'),
    (N'anh.nguyen@devspot.demo', N'DevSpot Geo Radar',
     N'Khám phá Creator và dự án theo bán kính.',
     N'API tìm kiếm dự án theo kỹ năng và tọa độ địa lý, hỗ trợ bản đồ cộng đồng.',
     N'https://github.com/hoanganh/geo-radar', N'https://example.com/radar-demo',
     N'Hải Châu, Đà Nẵng', 16.047100, 108.220800, 'Area')
) AS seed(Email, Title, Tagline, Description, RepositoryUrl, DemoUrl, Location, Latitude, Longitude, LocationPrivacy)
JOIN dbo.CreatorProfiles cp
    ON cp.UserId = (SELECT u.UserId FROM dbo.Users u WHERE u.Email = seed.Email)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Projects p WHERE p.CreatorId = cp.UserId AND p.Title = seed.Title
);

INSERT INTO dbo.CreatorSkills (CreatorId, SkillId, Level)
SELECT cp.UserId, s.SkillId, seed.Level
FROM (VALUES
    (N'quoc.le@devspot.demo', N'Kotlin', 4),
    (N'quoc.le@devspot.demo', N'ASP.NET Core', 4),
    (N'quoc.le@devspot.demo', N'SQL Server', 4),
    (N'tuan.kiet@devspot.demo', N'C++', 4),
    (N'tuan.kiet@devspot.demo', N'IoT', 5),
    (N'tuan.kiet@devspot.demo', N'TensorFlow Lite', 3),
    (N'mai.lan@devspot.demo', N'Android', 4),
    (N'mai.lan@devspot.demo', N'Kotlin', 4),
    (N'mai.lan@devspot.demo', N'MQTT', 3),
    (N'anh.nguyen@devspot.demo', N'ASP.NET Core', 4),
    (N'anh.nguyen@devspot.demo', N'SQL Server', 5),
    (N'anh.nguyen@devspot.demo', N'OSMDroid', 3)
) AS seed(Email, SkillName, Level)
JOIN dbo.Users u ON u.Email = seed.Email
JOIN dbo.CreatorProfiles cp ON cp.UserId = u.UserId
JOIN dbo.Skills s ON s.SkillName = seed.SkillName
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.CreatorSkills cs WHERE cs.CreatorId = cp.UserId AND cs.SkillId = s.SkillId
);

INSERT INTO dbo.ProjectSkills (ProjectId, SkillId)
SELECT p.ProjectId, s.SkillId
FROM (VALUES
    (N'DevSpot Location-Based Hub', N'quoc.le@devspot.demo', N'Kotlin'),
    (N'DevSpot Location-Based Hub', N'quoc.le@devspot.demo', N'OSMDroid'),
    (N'DevSpot Location-Based Hub', N'quoc.le@devspot.demo', N'SQL Server'),
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'C++'),
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'IoT'),
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'TensorFlow Lite'),
    (N'Smart Campus Monitor', N'mai.lan@devspot.demo', N'Android'),
    (N'Smart Campus Monitor', N'mai.lan@devspot.demo', N'MQTT'),
    (N'Smart Campus Monitor', N'mai.lan@devspot.demo', N'Python'),
    (N'DevSpot Geo Radar', N'anh.nguyen@devspot.demo', N'ASP.NET Core'),
    (N'DevSpot Geo Radar', N'anh.nguyen@devspot.demo', N'OSMDroid'),
    (N'DevSpot Geo Radar', N'anh.nguyen@devspot.demo', N'SQL Server')
) AS seed(ProjectTitle, CreatorEmail, SkillName)
JOIN dbo.Users owner ON owner.Email = seed.CreatorEmail
JOIN dbo.Projects p ON p.CreatorId = owner.UserId AND p.Title = seed.ProjectTitle
JOIN dbo.Skills s ON s.SkillName = seed.SkillName
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.ProjectSkills ps WHERE ps.ProjectId = p.ProjectId AND ps.SkillId = s.SkillId
);

INSERT INTO dbo.ProjectLikes (ProjectId, UserId)
SELECT p.ProjectId, u.UserId
FROM (VALUES
    (N'DevSpot Geo Radar', N'anh.nguyen@devspot.demo', N'minh.tran@nexusai.demo'),
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'minh.tran@nexusai.demo'),
    (N'DevSpot Location-Based Hub', N'quoc.le@devspot.demo', N'tuan.kiet@devspot.demo')
) AS seed(ProjectTitle, CreatorEmail, Email)
JOIN dbo.Users owner ON owner.Email = seed.CreatorEmail
JOIN dbo.Projects p ON p.CreatorId = owner.UserId AND p.Title = seed.ProjectTitle
JOIN dbo.Users u ON u.Email = seed.Email
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.ProjectLikes pl WHERE pl.ProjectId = p.ProjectId AND pl.UserId = u.UserId
);

INSERT INTO dbo.ProjectBookmarks (ProjectId, UserId)
SELECT p.ProjectId, u.UserId
FROM (VALUES
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'minh.tran@nexusai.demo'),
    (N'Smart Campus Monitor', N'mai.lan@devspot.demo', N'quoc.le@devspot.demo')
) AS seed(ProjectTitle, CreatorEmail, Email)
JOIN dbo.Users owner ON owner.Email = seed.CreatorEmail
JOIN dbo.Projects p ON p.CreatorId = owner.UserId AND p.Title = seed.ProjectTitle
JOIN dbo.Users u ON u.Email = seed.Email
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.ProjectBookmarks b WHERE b.ProjectId = p.ProjectId AND b.UserId = u.UserId
);

INSERT INTO dbo.Follows (FollowerId, FolloweeId)
SELECT follower.UserId, followee.UserId
FROM (VALUES
    (N'minh.tran@nexusai.demo', N'tuan.kiet@devspot.demo'),
    (N'minh.tran@nexusai.demo', N'quoc.le@devspot.demo'),
    (N'quoc.le@devspot.demo', N'mai.lan@devspot.demo')
) AS seed(FollowerEmail, FolloweeEmail)
JOIN dbo.Users follower ON follower.Email = seed.FollowerEmail
JOIN dbo.Users followee ON followee.Email = seed.FolloweeEmail
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Follows f WHERE f.FollowerId = follower.UserId AND f.FolloweeId = followee.UserId
);

INSERT INTO dbo.ProjectComments (ProjectId, UserId, Content)
SELECT p.ProjectId, u.UserId, seed.Content
FROM (VALUES
    (N'DevSpot Location-Based Hub', N'quoc.le@devspot.demo', N'minh.tran@nexusai.demo',
     N'Ý tưởng kết nối dự án với bản đồ rất hữu ích cho việc tìm kiếm ứng viên địa phương.'),
    (N'Cybersafe IoT / Edge AI', N'tuan.kiet@devspot.demo', N'quoc.le@devspot.demo',
     N'Phần xử lý trên thiết bị biên rất thú vị. Bạn có thể chia sẻ thêm về bộ dữ liệu không?'),
    (N'Smart Campus Monitor', N'mai.lan@devspot.demo', N'tuan.kiet@devspot.demo',
     N'Giao diện theo dõi cảm biến thời gian thực rất trực quan.')
) AS seed(ProjectTitle, CreatorEmail, Email, Content)
JOIN dbo.Users owner ON owner.Email = seed.CreatorEmail
JOIN dbo.Projects p ON p.CreatorId = owner.UserId AND p.Title = seed.ProjectTitle
JOIN dbo.Users u ON u.Email = seed.Email
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.ProjectComments c
    WHERE c.ProjectId = p.ProjectId AND c.UserId = u.UserId AND c.Content = seed.Content
);

INSERT INTO dbo.Messages (SenderId, ReceiverId, Content)
SELECT sender.UserId, receiver.UserId, seed.Content
FROM (VALUES
    (N'minh.tran@nexusai.demo', N'tuan.kiet@devspot.demo',
     N'Chào Kiệt, bên mình muốn trao đổi thêm về dự án Cybersafe IoT.'),
    (N'tuan.kiet@devspot.demo', N'minh.tran@nexusai.demo',
     N'Cảm ơn anh, em sẵn sàng trao đổi thêm về dự án.')
) AS seed(SenderEmail, ReceiverEmail, Content)
JOIN dbo.Users sender ON sender.Email = seed.SenderEmail
JOIN dbo.Users receiver ON receiver.Email = seed.ReceiverEmail
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Messages m
    WHERE m.SenderId = sender.UserId AND m.ReceiverId = receiver.UserId AND m.Content = seed.Content
);

INSERT INTO dbo.InterviewInvitations
    (RecruiterId, CreatorId, ProjectId, ScheduledAt, Location, SalaryOffer, Note)
SELECT recruiter.UserId, creator.UserId, p.ProjectId,
       '2027-01-15T09:30:00', N'Văn phòng Nexus AI Labs, Đà Nẵng',
       N'15.000.000 - 20.000.000 VNĐ/tháng',
       N'Bên mình ấn tượng với dự án Cybersafe IoT và muốn trao đổi về cơ hội cộng tác.'
FROM dbo.Users recruiter
JOIN dbo.Users creator ON creator.Email = N'tuan.kiet@devspot.demo'
JOIN dbo.RecruiterProfiles rp ON rp.UserId = recruiter.UserId
JOIN dbo.Projects p ON p.CreatorId = creator.UserId AND p.Title = N'Cybersafe IoT / Edge AI'
WHERE recruiter.Email = N'minh.tran@nexusai.demo'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.InterviewInvitations i
      WHERE i.RecruiterId = recruiter.UserId AND i.CreatorId = creator.UserId AND i.ProjectId = p.ProjectId
  );

INSERT INTO dbo.ProjectReviews (ProjectId, ReviewerId, Rating, Content)
SELECT p.ProjectId, u.UserId, 5, N'Dự án có ý tưởng thực tế, trình bày rõ ràng và có tiềm năng ứng dụng.'
FROM dbo.Projects p
JOIN dbo.Users u ON u.Email = N'minh.tran@nexusai.demo'
WHERE p.Title = N'Cybersafe IoT / Edge AI'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.ProjectReviews r WHERE r.ProjectId = p.ProjectId AND r.ReviewerId = u.UserId
  );

INSERT INTO dbo.Notifications (UserId, Title, Body, Type, ReferenceId)
SELECT creator.UserId, seed.Title, seed.Body, seed.Type, p.ProjectId
FROM (VALUES
    (N'tuan.kiet@devspot.demo', N'Cybersafe IoT / Edge AI', N'Lời mời phỏng vấn',
     N'Nexus AI Labs muốn trao đổi về dự án Cybersafe IoT / Edge AI.', 'Interview'),
    (N'quoc.le@devspot.demo', N'DevSpot Geo Radar', N'Dự án được quan tâm',
     N'Nexus AI Labs đã quan tâm đến DevSpot Geo Radar.', 'Project')
) AS seed(Email, ProjectTitle, Title, Body, Type)
JOIN dbo.Users creator ON creator.Email = seed.Email
JOIN dbo.Projects p ON p.CreatorId = creator.UserId AND p.Title = seed.ProjectTitle
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Notifications n
    WHERE n.UserId = creator.UserId AND n.Title = seed.Title AND n.Body = seed.Body
);

COMMIT TRANSACTION;
GO
