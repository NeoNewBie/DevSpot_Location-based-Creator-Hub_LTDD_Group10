package com.example.devspot.view

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.devspot.BuildConfig
import com.example.devspot.R
import com.example.devspot.data.DevSpotApiClient
import com.example.devspot.data.LoginRequest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.ArrayDeque

class MainActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val navigationHistory = ArrayDeque<Int>()
    private var currentLayout = 0
    private var recruiterMode = false
    private var registrationRecruiterMode: Boolean? = null
    private var isAuthenticated = false
    private var currentUserEmail: String? = null
    private var chatPartnerName = "Nexus AI Labs"
    private var chatPreviewMessage = "Bạn có thể tham gia phỏng vấn vào thứ 5 không?"
    private val demoCreators = listOf(
        DemoCreator(
            "nguyen-tuan-kiet",
            "Nguyễn Tuấn Kiệt",
            "Sinh viên CNTT",
            "ĐH Sư phạm Kỹ thuật Đà Nẵng",
            "Quan tâm đến AI/ML, hệ thống nhúng và các sản phẩm có tác động thực tế. Đang tìm cơ hội thực tập.",
            "Kotlin • Python • TensorFlow • IoT • Git"
        ),
        DemoCreator(
            "le-anh-quoc",
            "Lê Anh Quốc",
            "Android Developer",
            "Đại học Bách khoa Đà Nẵng",
            "Phát triển ứng dụng Android và dịch vụ bản đồ; yêu thích sản phẩm cộng đồng.",
            "Kotlin • Android • ASP.NET Core • OSMDroid"
        ),
        DemoCreator(
            "tran-gia-han",
            "Trần Gia Hân",
            "Sinh viên Software Engineering",
            "Đại học Duy Tân",
            "Xây dựng giải pháp IoT cho khuôn viên thông minh và hệ thống cảm biến.",
            "Python • MQTT • IoT • TensorFlow Lite"
        ),
        DemoCreator(
            "pham-minh-an",
            "Phạm Minh An",
            "AI/ML Developer",
            "Đại học Bách khoa Đà Nẵng",
            "Quan tâm đến machine learning, xử lý ngôn ngữ và sản phẩm AI hữu ích.",
            "Python • AI/ML • FastAPI • PyTorch"
        )
    )
    private val demoProjects = listOf(
        DemoProject(
            "DevSpot Location-Based Hub",
            "le-anh-quoc",
            "Nền tảng bản đồ kết nối Creator, dự án công nghệ và nhà tuyển dụng theo vị trí.",
            "Kotlin • Android • ASP.NET Core • SQL Server • OSMDroid"
        ),
        DemoProject(
            "Cybersafe IoT / Edge AI",
            "nguyen-tuan-kiet",
            "Thiết bị IoT phát hiện bất thường tại biên, gửi cảnh báo theo thời gian thực.",
            "Kotlin • Python • TensorFlow • IoT • MQTT"
        ),
        DemoProject(
            "Smart Campus Monitor",
            "tran-gia-han",
            "Theo dõi môi trường khuôn viên bằng cảm biến và dashboard trực quan.",
            "Python • MQTT • IoT • TensorFlow Lite"
        ),
        DemoProject(
            "AI Mentor Mini",
            "pham-minh-an",
            "Trợ lý AI mẫu giúp sinh viên tìm lộ trình học và tài liệu phù hợp.",
            "Python • PyTorch • FastAPI"
        )
    )
    private var selectedDemoCreator = demoCreators.first()
    private var selectedDemoProject = demoProjects.first()
    private var mapView: MapView? = null
    private var locationPermissionRequested = false
    private var activityResumed = false

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                setupMap()
            } else {
                locationPermissionRequested = true
                setupMap()
                Toast.makeText(
                    this,
                    "Bạn vẫn có thể xem Radar; vị trí hiện tại chưa được bật.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.bg_dark)
        Configuration.getInstance().userAgentValue =
            "${getString(R.string.app_name)}/${BuildConfig.VERSION_NAME} " +
                "(+https://github.com/NeoNewBie/DevSpot_Location-based-Creator-Hub_LTDD_Group10; $packageName)"
        showScreen(R.layout.activity_splash, addToHistory = false)
        mainHandler.postDelayed({
            if (!isFinishing && currentLayout == R.layout.activity_splash) {
                showScreen(R.layout.activity_login, addToHistory = false)
            }
        }, SPLASH_DURATION_MS)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    navigationHistory.isNotEmpty() ->
                        showScreen(navigationHistory.removeLast(), addToHistory = false)
                    isAuthenticated || currentLayout == R.layout.activity_login -> finish()
                    else -> showScreen(R.layout.activity_login, addToHistory = false)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        mapView?.onResume()
        if (currentLayout == R.layout.fragment_creator_home || currentLayout == R.layout.fragment_recruiter_home) {
            setupMap()
        }
    }

    override fun onPause() {
        activityResumed = false
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showScreen(layout: Int, addToHistory: Boolean = true) {
        if (addToHistory && currentLayout != 0 && currentLayout != layout) {
            navigationHistory.addLast(currentLayout)
        }
        mapView?.onPause()
        currentLayout = layout
        mapView = null
        setContentView(layout)
        applyWindowInsets()
        bindCommonActions()
        bindScreenActions(layout)
        populateLists(layout)
        updateNavigationSelection(layout)

        // Smooth entry animation for premium feel
        val content = findViewById<ViewGroup>(android.R.id.content)
        content.getChildAt(0)?.let { root ->
            root.alpha = 0f
            root.translationY = 35f
            root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        if (layout == R.layout.fragment_creator_home || layout == R.layout.fragment_recruiter_home) {
            setupMap()
            requestLocationPermissionIfNeeded()
        }
    }

    private fun shakeView(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 20f, -20f, 10f, -10f, 0f)
        animator.duration = 400
        animator.start()
    }

    private fun applyWindowInsets() {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun bindCommonActions() {
        bind(R.id.btnBack) { goBack() }
        bind(R.id.btnBackFromChat) { goBack() }

        bind(R.id.navMap) { openTab(homeLayout()) }
        bind(R.id.navClusters) { openTab(R.layout.fragment_cluster_nodes) }
        bind(R.id.navSignal) { openTab(R.layout.fragment_signal_stream) }
        bind(R.id.navSaved) { openTab(R.layout.fragment_saved_bookmarks) }
        bind(R.id.navMessages) { openTab(R.layout.fragment_inbox) }
        bind(R.id.btnHomeMessages) { openTab(R.layout.fragment_inbox) }
        bind(R.id.btnHomeNotifications) { openTab(R.layout.fragment_signal_stream) }
        bind(R.id.btnNavRadar) { openTab(homeLayout()) }
        bind(R.id.btnNavSaved) { openTab(R.layout.fragment_saved_bookmarks) }
        bind(R.id.btnNavCompany) {
            openTab(if (recruiterMode) R.layout.activity_company_profile else R.layout.fragment_creator_profile)
        }
        bind(R.id.navCreatorRadar) { openTab(R.layout.fragment_creator_home) }
        bind(R.id.navCreatorExplore) { openTab(R.layout.fragment_cluster_nodes) }
        bind(R.id.navCreatorProfile) { openTab(R.layout.fragment_creator_profile) }
        bind(R.id.navRecruiterRadar) { openTab(R.layout.fragment_recruiter_home) }
        bind(R.id.navRecruiterSaved) { openTab(R.layout.fragment_saved_bookmarks) }
        bind(R.id.navRecruiterCompany) { openTab(R.layout.activity_company_profile) }
        bind(R.id.btnHomeSettings) { showScreen(R.layout.fragment_system_settings) }
    }

    private fun bindScreenActions(layout: Int) {
        when (layout) {
            R.layout.activity_splash -> {
                bind(R.id.ivLogo) {
                    showScreen(R.layout.activity_login, addToHistory = false)
                }
                bind(R.id.main) {
                    showScreen(R.layout.activity_login, addToHistory = false)
                }
            }

            R.layout.activity_role_selection -> {
                bind(R.id.btnOpenLogin) { showScreen(R.layout.activity_login) }
                registrationRecruiterMode = null
                updateRoleSelection()
                bind(R.id.cardCreator) {
                    registrationRecruiterMode = false
                    updateRoleSelection()
                    Toast.makeText(this, "Đã chọn vai trò Creator", Toast.LENGTH_SHORT).show()
                }
                bind(R.id.cardRecruiter) {
                    registrationRecruiterMode = true
                    updateRoleSelection()
                    Toast.makeText(this, "Đã chọn vai trò Recruiter", Toast.LENGTH_SHORT).show()
                }
                bind(R.id.btnContinue) {
                    when (registrationRecruiterMode) {
                        true -> showScreen(R.layout.activity_register_recruiter)
                        false -> showScreen(R.layout.activity_register_creator)
                        null -> {
                            findViewById<TextView>(R.id.tvRoleSelectionFeedback)?.apply {
                                text = "Vui lòng chọn Creator hoặc Recruiter trước khi tiếp tục."
                                visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }

            R.layout.activity_login -> {
                bind(R.id.btnForgotPassword) { showScreen(R.layout.activity_password_reset) }
                bind(R.id.btnOpenRegistration) { openRegistrationRoleSelection() }
                bind(R.id.btnLogin) {
                    val email = findViewById<EditText>(R.id.etEmail)
                    val password = findViewById<EditText>(R.id.etPassword)
                    if (email.text.isNullOrBlank()) {
                        email.error = "Vui lòng nhập email"
                        return@bind
                    }
                    if (password.text.isNullOrBlank()) {
                        password.error = "Vui lòng nhập mật khẩu"
                        return@bind
                    }
                    val loginButton = findViewById<Button>(R.id.btnLogin)
                    val loginFeedback = findViewById<TextView>(R.id.tvLoginFeedback)
                    loginFeedback.visibility = View.GONE
                    loginButton.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            val response = DevSpotApiClient.service.login(
                                LoginRequest(email.text.toString().trim(), password.text.toString())
                            )
                            if (response.isSuccessful) {
                                val account = response.body()
                                if (account == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Máy chủ trả về dữ liệu đăng nhập không hợp lệ.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                recruiterMode = account.role.equals("Recruiter", ignoreCase = true)
                                currentUserEmail = account.email.trim().lowercase()
                                isAuthenticated = true
                                Toast.makeText(
                                    this@MainActivity,
                                    "Xin chào ${account.fullName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openHome()
                            } else {
                                val message = when (response.code()) {
                                    401 -> "Email hoặc mật khẩu không đúng."
                                    503 -> "API không kết nối được SQL Server. Hãy kiểm tra backend."
                                    else -> "Đăng nhập thất bại (HTTP ${response.code()})."
                                }
                                loginFeedback.text = message
                                loginFeedback.visibility = View.VISIBLE
                            }
                        } catch (_: java.io.IOException) {
                            loginFeedback.text = "Không kết nối được API. Hãy khởi chạy backend trên máy tính."
                            loginFeedback.visibility = View.VISIBLE
                        } finally {
                            loginButton.isEnabled = true
                        }
                    }
                }
            }

            R.layout.activity_password_reset -> bind(R.id.btnSendResetLink) {
                val email = findViewById<EditText>(R.id.etResetEmail)
                val feedback = findViewById<TextView>(R.id.tvResetFeedback)
                if (email.text.isNullOrBlank()) {
                    email.error = "Vui lòng nhập email"
                } else {
                    feedback.text = "Giao diện đã sẵn sàng; dịch vụ gửi email chưa được kết nối."
                    feedback.visibility = View.VISIBLE
                }
            }

            R.layout.activity_register_creator -> {
                bind(R.id.btnChangeRole) { openRegistrationRoleSelection() }
                bind(R.id.btnOpenLogin) { showScreen(R.layout.activity_login) }
                bind(R.id.btnRegister) {
                    val name = findViewById<EditText>(R.id.etName)
                    val email = findViewById<EditText>(R.id.etEmail)
                    val password = findViewById<EditText>(R.id.etPassword)
                    when {
                        name.text.isNullOrBlank() -> name.error = "Vui lòng nhập họ tên"
                        email.text.isNullOrBlank() -> email.error = "Vui lòng nhập email"
                        password.text.isNullOrBlank() -> password.error = "Vui lòng nhập mật khẩu"
                        !findViewById<CheckBox>(R.id.cbTerms).isChecked ->
                            Toast.makeText(this, "Vui lòng đồng ý điều khoản để tiếp tục.", Toast.LENGTH_SHORT).show()
                        else -> {
                            Toast.makeText(
                                this,
                                "Đăng ký chưa kết nối API nên tài khoản chưa được tạo. Hãy đăng nhập bằng tài khoản demo.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            R.layout.activity_register_recruiter -> {
                bind(R.id.btnChangeRole) { openRegistrationRoleSelection() }
                bind(R.id.btnOpenLogin) { showScreen(R.layout.activity_login) }
                bind(R.id.btnRegisterRecruiter) {
                    val name = findViewById<EditText>(R.id.etRepresentativeName)
                    val company = findViewById<EditText>(R.id.etCompanyName)
                    val email = findViewById<EditText>(R.id.etCompanyEmail)
                    val password = findViewById<EditText>(R.id.etPassword)
                    when {
                        name.text.isNullOrBlank() -> name.error = "Vui lòng nhập họ tên"
                        company.text.isNullOrBlank() -> company.error = "Vui lòng nhập tên công ty"
                        email.text.isNullOrBlank() -> email.error = "Vui lòng nhập email"
                        password.text.isNullOrBlank() -> password.error = "Vui lòng nhập mật khẩu"
                        !findViewById<CheckBox>(R.id.cbTerms).isChecked ->
                            Toast.makeText(this, "Vui lòng đồng ý điều khoản để tiếp tục.", Toast.LENGTH_SHORT).show()
                        else -> {
                            Toast.makeText(
                                this,
                                "Đăng ký chưa kết nối API nên tài khoản chưa được tạo. Hãy đăng nhập bằng tài khoản demo.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            R.layout.fragment_creator_home -> {
                bind(R.id.btnDropNewPin) { showScreen(R.layout.fragment_drop_project_pin) }
                bind(R.id.tvActiveProjectTitle) { showScreen(R.layout.activity_project_detail) }
                bind(R.id.tvLocationStatus) { openLocationSettingsOrRequestPermission() }
            }

            R.layout.fragment_recruiter_home -> {
                bind(R.id.btnFilterRadar) { showScreen(R.layout.bottom_sheet_filter_radar) }
                bind(R.id.btnOpenCandidate) { showScreen(R.layout.activity_creator_public_profile) }
                bind(R.id.tvLocationStatus) { openLocationSettingsOrRequestPermission() }
                findViewById<EditText>(R.id.etSearchTalent)?.setOnEditorActionListener { _, _, _ ->
                    Toast.makeText(this, "Bộ lọc từ khóa được áp dụng trong bản xem trước.", Toast.LENGTH_SHORT).show()
                    true
                }
            }

            R.layout.bottom_sheet_filter_radar -> bind(R.id.btnApplyFilter) {
                Toast.makeText(this, "Bộ lọc đã được áp dụng cho phiên xem trước.", Toast.LENGTH_SHORT).show()
                goBack()
            }

            R.layout.fragment_creator_profile -> {
                bind(R.id.btnSettings) { showScreen(R.layout.fragment_system_settings) }
                bind(R.id.btnEditProfile) { showScreen(R.layout.fragment_edit_profile) }
                bind(R.id.btnAddNewProject) { showScreen(R.layout.fragment_drop_project_pin) }
            }

            R.layout.fragment_edit_profile -> bind(R.id.btnSaveProfile) {
                Toast.makeText(this, "Bản xem trước chưa lưu hồ sơ vào máy chủ.", Toast.LENGTH_LONG).show()
                goBack()
            }

            R.layout.fragment_drop_project_pin -> bind(R.id.btnSubmitPin) {
                val projectName = findViewById<EditText>(R.id.etProjectName)
                val tagline = findViewById<EditText>(R.id.etTagline)
                when {
                    projectName.text.isNullOrBlank() -> projectName.error = "Vui lòng nhập tên dự án"
                    tagline.text.isNullOrBlank() -> tagline.error = "Vui lòng nhập mô tả ngắn"
                    else -> {
                        Toast.makeText(this, "Ghim chỉ được xem trước; chưa lưu vào máy chủ.", Toast.LENGTH_LONG).show()
                        openHome()
                    }
                }
            }

            R.layout.fragment_geo_privacy -> bind(R.id.btnUpdatePrivacy) {
                Toast.makeText(this, "Tùy chọn quyền riêng tư được lưu trong phiên xem trước.", Toast.LENGTH_SHORT).show()
                goBack()
            }

            R.layout.fragment_system_settings -> {
                bind(R.id.btnNavPrivacy) { showScreen(R.layout.fragment_geo_privacy) }
                bind(R.id.btnNavLogout) { showScreen(R.layout.activity_logout_confirm) }
                bind(R.id.btnChangePassword) { showScreen(R.layout.activity_password_reset) }
                bind(R.id.btnDeleteAccount) {
                    AlertDialog.Builder(this)
                        .setTitle("Xóa tài khoản")
                        .setMessage("Chức năng xóa tài khoản chưa kết nối máy chủ.")
                        .setNegativeButton("Đóng", null)
                        .show()
                }
            }

            R.layout.activity_logout_confirm -> {
                bind(R.id.btnCancelLogout) { goBack() }
                bind(R.id.btnConfirmLogout) {
                    Toast.makeText(this, "Đã kết thúc phiên xem trước.", Toast.LENGTH_SHORT).show()
                    isAuthenticated = false
                    currentUserEmail = null
                    recruiterMode = false
                    navigationHistory.clear()
                    showScreen(R.layout.activity_login, addToHistory = false)
                }
            }

            R.layout.fragment_signal_stream -> {
                bind(R.id.btnMarkAllRead) {
                    findViewById<TextView>(R.id.btnMarkAllRead).text = "✓ ĐÃ ĐỌC TẤT CẢ"
                }
                bind(R.id.btnSearchSignal) { showScreen(R.layout.fragment_inbox) }
                bind(R.id.btnSignalStatus) { showUiState("Radar đang hoạt động", "Bạn đang xem dữ liệu mẫu trong giao diện xem trước.", "QUAY LẠI") }
            }

            R.layout.activity_project_detail -> {
                findViewById<TextView>(R.id.tvProjectTitle)?.text = selectedDemoProject.title
                findViewById<TextView>(R.id.tvAuthorName)?.text =
                    "Tác giả: ${findDemoCreator(selectedDemoProject.creatorId)?.name.orEmpty()}"
                findViewById<TextView>(R.id.tvDescription)?.text = selectedDemoProject.description
                findViewById<TextView>(R.id.tvTechStack)?.text = selectedDemoProject.techStack
                findViewById<Button>(R.id.btnActionInvite)?.text =
                    if (recruiterMode) "⚡ GỬI LỜI MỜI PHỎNG VẤN TRỰC TIẾP" else "✦ ĐÁNH GIÁ DỰ ÁN"
                bind(R.id.tvAuthorName) {
                    findDemoCreator(selectedDemoProject.creatorId)?.let(::showCreatorProfile)
                }
                bind(R.id.btnBookmark) { toggleBookmark() }
                bind(R.id.btnLikeProject) { toggleButton(R.id.btnLikeProject, "♡ Thích", "♥ Đã thích") }
                bind(R.id.btnFollowCreator) { toggleButton(R.id.btnFollowCreator, "＋ Theo dõi", "✓ Đang theo dõi") }
                bind(R.id.btnOpenComments) {
                    findViewById<EditText>(R.id.etProjectComment).requestFocus()
                }
                bind(R.id.btnPostComment) {
                    val comment = findViewById<EditText>(R.id.etProjectComment)
                    if (comment.text.isNullOrBlank()) {
                        comment.error = "Nhập nội dung bình luận"
                    } else {
                        findViewById<TextView>(R.id.tvCommentFeedback).apply {
                            text = "Bình luận chỉ hiển thị trong phiên xem trước."
                            visibility = View.VISIBLE
                        }
                        comment.text.clear()
                    }
                }
                bind(R.id.btnActionInvite) {
                    if (recruiterMode) showInterviewInvite(findDemoCreator(selectedDemoProject.creatorId))
                    else showScreen(R.layout.activity_project_review)
                }
            }

            R.layout.activity_project_review -> bind(R.id.btnSubmitReview) {
                val comment = findViewById<EditText>(R.id.etReviewComment)
                val rating = findViewById<RatingBar>(R.id.ratingBar)
                if (rating.rating == 0f) {
                    Toast.makeText(this, "Hãy chọn mức đánh giá trước.", Toast.LENGTH_SHORT).show()
                } else if (comment.text.isNullOrBlank()) {
                    comment.error = "Vui lòng nhập nhận xét"
                } else {
                    Toast.makeText(this, "Đánh giá chưa được gửi lên máy chủ.", Toast.LENGTH_LONG).show()
                    goBack()
                }
            }

            R.layout.activity_interview_dispatch -> {
                findViewById<TextView>(R.id.tvCandidateName)?.text = selectedDemoCreator.name
                findViewById<TextView>(R.id.tvCandidateUniversity)?.text =
                    "${selectedDemoCreator.headline} • ${selectedDemoCreator.school}"
                findViewById<TextView>(R.id.tvTargetProject)?.text =
                    "🎯 Target: ${demoProjects.firstOrNull { it.creatorId == selectedDemoCreator.id }?.title ?: "Cơ hội phù hợp"}"
                bind(R.id.btnSendInterview) {
                    Toast.makeText(this, "Lời mời chỉ được xem trước; chưa gửi tới ứng viên.", Toast.LENGTH_LONG).show()
                    showScreen(R.layout.activity_interview_success)
                }
            }

            R.layout.activity_interview_success -> bind(R.id.btnGoToChat) {
                openChatWith(
                    selectedDemoCreator,
                    "Mình muốn trao đổi thêm về lịch phỏng vấn và cơ hội tuyển dụng."
                )
            }

            R.layout.activity_interview_response -> {
                bind(R.id.btnAcceptInterview) { updateInterviewResponse("Bạn đã chấp nhận lịch phỏng vấn (bản xem trước).") }
                bind(R.id.btnProposeInterview) {
                    val alternative = findViewById<EditText>(R.id.etAlternativeTime)
                    if (alternative.text.isNullOrBlank()) {
                        alternative.error = "Nhập thời gian bạn đề xuất"
                    } else {
                        updateInterviewResponse("Đề xuất lịch chỉ hiển thị trong phiên xem trước.")
                    }
                }
                bind(R.id.btnDeclineInterview) { updateInterviewResponse("Đã ghi nhận từ chối trong phiên xem trước.") }
            }

            R.layout.activity_chat_detail -> {
                findViewById<TextView>(R.id.tvChatPartnerName)?.text = chatPartnerName
                findViewById<TextView>(R.id.tvChatStatus)?.text = "Cuộc trò chuyện mẫu • chưa đồng bộ máy chủ"
                bind(R.id.btnSendMessage) {
                    val input = findViewById<EditText>(R.id.etChatMessage)
                    if (input.text.isNullOrBlank()) {
                        input.error = "Nhập nội dung tin nhắn"
                    } else {
                        val adapter = findViewById<RecyclerView>(R.id.rvChatMessages).adapter as? UiRowsAdapter
                        adapter?.addRow(UiRow("Bạn • Bản xem trước", input.text.toString()))
                        input.text.clear()
                        findViewById<TextView>(R.id.tvChatFeedback).apply {
                            text = "Tin nhắn chưa được gửi tới máy chủ."
                            visibility = View.VISIBLE
                        }
                    }
                }
                bind(R.id.btnAttachFile) {
                    Toast.makeText(this, "Đính kèm tệp chưa được hỗ trợ trong bản xem trước.", Toast.LENGTH_SHORT).show()
                }
            }

            R.layout.activity_company_profile -> {
                bind(R.id.btnCompanyMessages) { showScreen(R.layout.fragment_inbox) }
                bind(R.id.btnEditCompany) {
                    Toast.makeText(this, "Chỉnh sửa hồ sơ công ty chưa kết nối.", Toast.LENGTH_SHORT).show()
                }
            }

            R.layout.activity_creator_public_profile -> {
                updatePublicCreatorProfile()
                findViewById<Button>(R.id.btnSavePublicCreator)?.visibility =
                    if (recruiterMode) View.VISIBLE else View.GONE
                findViewById<Button>(R.id.btnFollowPublicCreator)?.visibility =
                    if (recruiterMode) View.GONE else View.VISIBLE
                findViewById<Button>(R.id.btnInvitePublicCreator)?.visibility =
                    if (recruiterMode) View.VISIBLE else View.GONE
                updatePublicCreatorSaveButton()
                updateCreatorConnectionButton()
                bind(R.id.btnFollowPublicCreator) { toggleCreatorConnection() }
                bind(R.id.btnMessagePublicCreator) {
                    openChatWith(selectedDemoCreator, "Chào ${selectedDemoCreator.name}, mình muốn kết nối trên DevSpot.")
                }
                bind(R.id.btnInvitePublicCreator) { showInterviewInvite(selectedDemoCreator) }
                bind(R.id.btnSavePublicCreator) { toggleSavedCreator() }
            }

            R.layout.activity_ui_state -> {
                bind(R.id.btnStateBack) { goBack() }
                bind(R.id.btnStateAction) { goBack() }
            }
        }
    }

    private fun populateLists(layout: Int) {
        when (layout) {
            R.layout.fragment_inbox -> {
                val conversations = if (recruiterMode) {
                    listOf(
                        UiRow("Nguyễn Tuấn Kiệt • Creator", "Cảm ơn anh/chị, tôi có thể trao đổi vào thứ 5."),
                        UiRow("Lê Anh Quốc • Creator", "Tôi vừa cập nhật dự án DevSpot Geo Radar.")
                    )
                } else {
                    listOf(
                        UiRow("Nexus AI Labs • Nhà tuyển dụng", "Bạn có thể tham gia phỏng vấn vào thứ 5 không?"),
                        UiRow("Techcombank Digital • Nhà tuyển dụng", "Đội ngũ đã xem portfolio của bạn.")
                    )
                }
                setRows(R.id.rvInboxList, conversations) { row ->
                    chatPartnerName = row.title.substringBefore(" •")
                    chatPreviewMessage = row.subtitle
                    showScreen(R.layout.activity_chat_detail)
                }
            }

            R.layout.fragment_signal_stream -> {
                findViewById<TextView>(R.id.tvSignalSummary)?.text =
                    if (recruiterMode) "● THÔNG BÁO RECRUITER • 3 MỚI" else "● THÔNG BÁO CREATOR • 3 MỚI"
                val notifications = if (recruiterMode) {
                    listOf(
                        UiRow("PHẢN HỒI LỜI MỜI • Hôm nay", "Nguyễn Tuấn Kiệt đã phản hồi lời mời phỏng vấn."),
                        UiRow("CREATOR MỚI • 1h trước", "Lê Anh Quốc vừa cập nhật hồ sơ và dự án."),
                        UiRow("DỰ ÁN MỚI • Hôm qua", "Có dự án Android mới trong khu vực Radar.")
                    )
                } else {
                    listOf(
                        UiRow("LỜI MỜI PHỎNG VẤN • 03h trước", "Nexus AI Labs mời bạn trao đổi về dự án của mình."),
                        UiRow("DỰ ÁN ĐƯỢC QUAN TÂM • Hôm nay", "Nexus AI Labs đã xem DevSpot Location-Based Hub."),
                        UiRow("KẾT NỐI MỚI • Hôm qua", "Một Recruiter mới đang theo dõi hồ sơ của bạn.")
                    )
                }
                setRows(R.id.rvSignalStream, notifications) { row ->
                    when {
                        recruiterMode && row.title.contains("DỰ ÁN") ->
                            showScreen(R.layout.activity_project_detail)
                        recruiterMode ->
                            showScreen(R.layout.activity_creator_public_profile)
                        row.title.contains("LỜI MỜI") ->
                            showScreen(R.layout.activity_interview_response)
                        row.title.contains("DỰ ÁN") ->
                            showScreen(R.layout.activity_project_detail)
                        else -> showScreen(R.layout.activity_creator_public_profile)
                    }
                }
            }

            R.layout.fragment_saved_bookmarks -> {
                val savedIds: Set<String>
                val rows: List<UiRow>
                if (recruiterMode) {
                    savedIds = getSavedIds(SAVED_CREATORS_KEY)
                    findViewById<TextView>(R.id.tvSavedTitle)?.text = "CREATOR ĐÃ LƯU"
                    findViewById<TextView>(R.id.tvSavedCount)?.text =
                        "${savedIds.size} Creator"
                    findViewById<TextView>(R.id.tvSavedDescription)?.text =
                        "Hồ sơ Creator bạn đã đánh dấu khi xem Radar."
                    findViewById<TextView>(R.id.tvSavedEmptyTitle)?.text =
                        "Chưa có Creator nào được lưu"
                    findViewById<TextView>(R.id.tvSavedEmptyDescription)?.text =
                        "Mở hồ sơ Creator từ Radar rồi chọn “Lưu hồ sơ Creator”."
                    rows = demoCreators
                        .filter { it.id in savedIds }
                        .map { creator ->
                            UiRow(creator.name, "${creator.headline} • ${creator.school}", creator.id)
                        }
                } else {
                    savedIds = getSavedIds(SAVED_PROJECTS_KEY)
                    findViewById<TextView>(R.id.tvSavedTitle)?.text = "DỰ ÁN ĐÃ LƯU"
                    findViewById<TextView>(R.id.tvSavedCount)?.text =
                        "${savedIds.size} dự án"
                    findViewById<TextView>(R.id.tvSavedDescription)?.text =
                        "Các dự án bạn đã đánh dấu từ trang chi tiết."
                    findViewById<TextView>(R.id.tvSavedEmptyTitle)?.text =
                        "Chưa có dự án nào được lưu"
                    findViewById<TextView>(R.id.tvSavedEmptyDescription)?.text =
                        "Mở chi tiết dự án rồi chạm biểu tượng lưu để thêm vào đây."
                    rows = savedIds.map { projectTitle ->
                        UiRow(projectTitle, "Dự án đã lưu trên thiết bị")
                    }
                }
                findViewById<View>(R.id.layoutEmpty)?.visibility =
                    if (rows.isEmpty()) View.VISIBLE else View.GONE
                setRows(R.id.rvSavedProjects, rows) { row ->
                    if (recruiterMode) {
                        findDemoCreator(row.key ?: row.title)?.let(::showCreatorProfile)
                    } else {
                        demoProjects.firstOrNull { it.title == row.title }?.let {
                            selectedDemoProject = it
                            showScreen(R.layout.activity_project_detail)
                        }
                    }
                }
            }

            R.layout.activity_project_detail -> {
                updateProjectDetailFromSelection()
                updateProjectBookmarkButton()
            }

            R.layout.fragment_cluster_nodes -> setRows(
                R.id.rvClusters,
                listOf(
                    UiRow("UTE Đà Nẵng", "12 Creator • Android, Kotlin, IoT"),
                    UiRow("Đại học Bách khoa Đà Nẵng", "18 Creator • AI/ML, Backend"),
                    UiRow("Khu Công Nghệ Cao", "7 doanh nghiệp • 14 cơ hội thực tập")
                )
            ) { openHome() }

            R.layout.fragment_creator_profile -> setRows(
                R.id.rvPinnedProjects,
                listOf(
                    UiRow("DevSpot Geo Radar", "Kotlin • ASP.NET Core • OSMDroid"),
                    UiRow("Cybersafe IoT", "C++ • Edge AI • MQTT")
                )
            ) { showScreen(R.layout.activity_project_detail) }

            R.layout.activity_creator_public_profile -> {
                updatePublicCreatorProfile()
                val projects = demoProjects.filter { it.creatorId == selectedDemoCreator.id }
                setRows(
                    R.id.rvPublicCreatorProjects,
                    projects.map { UiRow(it.title, it.techStack) }
                ) { row ->
                    demoProjects.firstOrNull { it.title == row.title }?.let {
                        selectedDemoProject = it
                        showScreen(R.layout.activity_project_detail)
                    }
                }
            }

            R.layout.activity_chat_detail -> {
                setRows(
                    R.id.rvChatMessages,
                    listOf(
                        UiRow(chatPartnerName, chatPreviewMessage),
                        UiRow("Bạn", "Cảm ơn, mình sẵn sàng trao đổi.")
                    )
                )
                findViewById<TextView>(R.id.tvChatEmpty)?.visibility = View.GONE
            }
        }
    }

    private fun setRows(
        recyclerId: Int,
        rows: List<UiRow>,
        onClick: (UiRow) -> Unit = {}
    ) {
        findViewById<RecyclerView>(recyclerId)?.adapter = UiRowsAdapter(this, rows, onClick)
    }

    private fun setupMap() {
        val view = findViewById<MapView>(R.id.mapView) ?: return
        val isNewMap = mapView !== view
        mapView = view
        if (activityResumed && isNewMap) view.onResume()
        view.setTileSource(TileSourceFactory.OpenTopo)
        view.setMultiTouchControls(true)
        val location = lastKnownLocation()
        val locationStatus = findViewById<TextView>(R.id.tvLocationStatus)
        val hasPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        locationStatus?.text = when {
            !hasPermission -> "Vị trí chưa được cấp • Chạm để thử lại"
            location == null -> "GPS đang tắt/chưa có vị trí gần đây • Chạm để cài đặt"
            else -> "Đã xác định vị trí gần đây"
        }
        val center = location?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(16.0759, 108.1534)
        view.controller.setZoom(14.0)
        view.controller.setCenter(center)
        view.overlays.removeAll { it is Marker }
        view.overlays.add(Marker(view).apply {
            position = center
            title = if (location == null) "Khu vực Đà Nẵng" else "Vị trí gần đây"
            snippet = "Vị trí tham chiếu"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
        val demoPins = demoMapPins()
        findViewById<TextView>(R.id.tvMapDemoBadge)?.text =
            "${demoPins.size} GHIM MẪU • chạm ghim để xem"
        demoPins.forEach { pin ->
            view.overlays.add(Marker(view).apply {
                position = GeoPoint(center.latitude + pin.latitudeOffset, center.longitude + pin.longitudeOffset)
                title = pin.item.title
                snippet = pin.item.description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    showMapPinActions(pin.item)
                    true
                }
            })
        }
        view.invalidate()
    }

    private fun demoMapPins(): List<MapDemoPin> {
        val projects = demoProjects.associateBy { it.title }
        val creators = demoCreators.associateBy { it.id }
        return if (recruiterMode) {
            listOf(
                MapDemoPin(DemoMapItem.Creator(creators.getValue("nguyen-tuan-kiet")), 0.006, -0.005),
                MapDemoPin(DemoMapItem.Creator(creators.getValue("le-anh-quoc")), -0.006, 0.004),
                MapDemoPin(DemoMapItem.Creator(creators.getValue("tran-gia-han")), 0.002, 0.009),
                MapDemoPin(DemoMapItem.Creator(creators.getValue("pham-minh-an")), -0.009, -0.008)
            )
        } else {
            listOf(
                MapDemoPin(DemoMapItem.Project(projects.getValue("DevSpot Location-Based Hub")), 0.006, -0.005),
                MapDemoPin(DemoMapItem.Project(projects.getValue("Cybersafe IoT / Edge AI")), -0.006, 0.004),
                MapDemoPin(DemoMapItem.Creator(creators.getValue("tran-gia-han")), 0.002, 0.009),
                MapDemoPin(DemoMapItem.Creator(creators.getValue("pham-minh-an")), -0.009, -0.008)
            )
        }
    }

    private fun showMapPinActions(item: DemoMapItem) {
        when (item) {
            is DemoMapItem.Project -> {
                val project = item.project
                val creator = findDemoCreator(project.creatorId) ?: return
                AlertDialog.Builder(this)
                    .setTitle(project.title)
                    .setMessage("${project.description}\n\nTác giả: ${creator.name}\n${project.techStack}")
                    .setPositiveButton("Chi tiết dự án") { _, _ ->
                        selectedDemoProject = project
                        showScreen(R.layout.activity_project_detail)
                    }
                    .setNeutralButton("Xem CV tác giả") { _, _ -> showCreatorProfile(creator) }
                    .setNegativeButton("Đóng", null)
                    .show()
            }

            is DemoMapItem.Creator -> {
                val creator = item.creator
                val dialog = AlertDialog.Builder(this)
                    .setTitle(creator.name)
                    .setMessage("${creator.headline} • ${creator.school}\n\n${creator.skills}\n\n${creator.bio}")
                    .setPositiveButton("Xem CV") { _, _ -> showCreatorProfile(creator) }
                if (recruiterMode) {
                    dialog.setNeutralButton("Mời tuyển dụng") { _, _ -> showInterviewInvite(creator) }
                    dialog.setNegativeButton("Đóng", null)
                } else {
                    dialog.setNeutralButton("Kết bạn") { _, _ ->
                        selectedDemoCreator = creator
                        toggleCreatorConnection()
                    }
                    dialog.setNegativeButton("Nhắn tin") { _, _ ->
                        openChatWith(creator, "Chào ${creator.name}, mình muốn kết nối trên DevSpot.")
                    }
                }
                dialog.show()
            }
        }
    }

    private fun showCreatorProfile(creator: DemoCreator) {
        selectedDemoCreator = creator
        showScreen(R.layout.activity_creator_public_profile)
    }

    private fun showInterviewInvite(creator: DemoCreator?) {
        selectedDemoCreator = creator ?: selectedDemoCreator
        selectedDemoProject = demoProjects.firstOrNull { it.creatorId == selectedDemoCreator.id }
            ?: selectedDemoProject
        showScreen(R.layout.activity_interview_dispatch)
    }

    private fun openChatWith(creator: DemoCreator, preview: String) {
        chatPartnerName = creator.name
        chatPreviewMessage = preview
        showScreen(R.layout.activity_chat_detail)
    }

    private fun findDemoCreator(idOrName: String): DemoCreator? =
        demoCreators.firstOrNull { it.id == idOrName || it.name == idOrName }

    private fun updateProjectDetailFromSelection() {
        val creator = findDemoCreator(selectedDemoProject.creatorId) ?: return
        selectedDemoCreator = creator
        findViewById<TextView>(R.id.tvProjectTitle)?.text = selectedDemoProject.title
        findViewById<TextView>(R.id.tvAuthorName)?.text =
            "Tác giả: ${creator.name} • ${creator.school}"
        findViewById<TextView>(R.id.tvDescription)?.text = selectedDemoProject.description
        findViewById<TextView>(R.id.tvTechStack)?.text = selectedDemoProject.techStack
        findViewById<Button>(R.id.btnActionInvite)?.text =
            if (recruiterMode) "⚡ GỬI LỜI MỜI PHỎNG VẤN TRỰC TIẾP" else "✦ ĐÁNH GIÁ DỰ ÁN"
    }

    private fun updatePublicCreatorProfile() {
        findViewById<TextView>(R.id.tvPublicCreatorName)?.text = selectedDemoCreator.name
        findViewById<TextView>(R.id.tvPublicCreatorHeadline)?.text =
            "${selectedDemoCreator.headline} • ${selectedDemoCreator.school}"
        findViewById<TextView>(R.id.tvPublicCreatorBio)?.text = selectedDemoCreator.bio
        findViewById<TextView>(R.id.tvPublicCreatorSkills)?.text = selectedDemoCreator.skills
    }

    private fun lastKnownLocation(): android.location.Location? {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        return manager.getLastKnownLocation(provider)
    }

    private fun requestLocationPermissionIfNeeded() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse && !locationPermissionRequested) {
            locationPermissionRequested = true
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun openLocationSettingsOrRequestPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            locationPermissionRequested = false
            requestLocationPermissionIfNeeded()
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun updateInterviewResponse(message: String) {
        findViewById<TextView>(R.id.tvInterviewResponseStatus)?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun updateRoleSelection() {
        val selectedRole = registrationRecruiterMode
        val creatorSelected = selectedRole == false
        val recruiterSelected = selectedRole == true
        styleRoleCard(R.id.cardCreator, creatorSelected)
        styleRoleCard(R.id.cardRecruiter, recruiterSelected)
        findViewById<ImageView>(R.id.ivRoleCreator)?.imageTintList =
            ContextCompat.getColorStateList(this, if (creatorSelected) R.color.neon_green else R.color.text_sub)
        findViewById<ImageView>(R.id.ivRoleRecruiter)?.imageTintList =
            ContextCompat.getColorStateList(this, if (recruiterSelected) R.color.neon_green else R.color.text_sub)
        findViewById<ImageView>(R.id.ivRoleCreatorCheck)?.visibility =
            if (creatorSelected) View.VISIBLE else View.INVISIBLE
        findViewById<ImageView>(R.id.ivRoleRecruiterCheck)?.visibility =
            if (recruiterSelected) View.VISIBLE else View.INVISIBLE
        findViewById<ImageView>(R.id.ivRoleCreatorCheck)?.imageTintList =
            ContextCompat.getColorStateList(this, R.color.neon_green)
        findViewById<ImageView>(R.id.ivRoleRecruiterCheck)?.imageTintList =
            ContextCompat.getColorStateList(this, R.color.neon_green)
        findViewById<TextView>(R.id.tvRoleCreatorTitle)?.setTextColor(
            ContextCompat.getColor(this, if (creatorSelected) R.color.neon_green else R.color.text_main)
        )
        findViewById<TextView>(R.id.tvRoleRecruiterTitle)?.setTextColor(
            ContextCompat.getColor(this, if (recruiterSelected) R.color.neon_green else R.color.text_main)
        )
        findViewById<TextView>(R.id.tvRoleCreatorSubtitle)?.setTextColor(
            ContextCompat.getColor(this, if (creatorSelected) R.color.neon_green else R.color.text_sub)
        )
        findViewById<TextView>(R.id.tvRoleRecruiterSubtitle)?.setTextColor(
            ContextCompat.getColor(this, if (recruiterSelected) R.color.neon_green else R.color.text_sub)
        )
        findViewById<TextView>(R.id.tvRoleCreatorFeatures)?.setTextColor(
            ContextCompat.getColor(this, if (creatorSelected) R.color.neon_green else R.color.text_sub)
        )
        findViewById<TextView>(R.id.tvRoleRecruiterFeatures)?.setTextColor(
            ContextCompat.getColor(this, if (recruiterSelected) R.color.neon_green else R.color.text_sub)
        )
        findViewById<Button>(R.id.btnContinue)?.apply {
            isEnabled = selectedRole != null
            text = when (selectedRole) {
                true -> "Tiếp tục với Recruiter →"
                false -> "Tiếp tục với Creator →"
                null -> "Chọn một vai trò để tiếp tục"
            }
            backgroundTintList = ContextCompat.getColorStateList(
                this@MainActivity,
                if (selectedRole == null) R.color.text_sub else R.color.neon_green
            )
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (selectedRole == null) R.color.text_main else R.color.bg_dark
                )
            )
        }
        findViewById<TextView>(R.id.tvRoleSelectionFeedback)?.apply {
            if (selectedRole == null) {
                text = "Chọn một vai trò để bật nút tiếp tục."
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun styleRoleCard(cardId: Int, selected: Boolean) {
        val density = resources.displayMetrics.density
        findViewById<View>(cardId)?.apply {
            isSelected = selected
            background = GradientDrawable().apply {
                setColor(
                    if (selected) android.graphics.Color.parseColor("#1C2A20")
                    else ContextCompat.getColor(this@MainActivity, R.color.bg_panel)
                )
                cornerRadius = 12f * density
                setStroke(
                    ((if (selected) 2 else 1) * density).toInt(),
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (selected) R.color.neon_green else R.color.text_sub
                    )
                )
            }
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
        }
    }

    private fun openRegistrationRoleSelection() {
        registrationRecruiterMode = null
        showScreen(R.layout.activity_role_selection)
    }

    private fun showUiState(title: String, message: String, action: String) {
        showScreen(R.layout.activity_ui_state)
        findViewById<TextView>(R.id.tvStateTitle).text = title
        findViewById<TextView>(R.id.tvStateMessage).text = message
        findViewById<Button>(R.id.btnStateAction).text = action
    }

    private fun toggleButton(id: Int, initialText: String, selectedText: String) {
        val button = findViewById<Button>(id) ?: return
        val selected = button.tag == true
        button.tag = !selected
        button.text = if (selected) initialText else selectedText
    }

    private fun toggleBookmark() {
        val bookmark = findViewById<ImageView>(R.id.btnBookmark) ?: return
        val projectTitle = findViewById<TextView>(R.id.tvProjectTitle)?.text?.toString()
            ?.takeIf { it.isNotBlank() } ?: return
        val savedIds = getSavedIds(SAVED_PROJECTS_KEY).toMutableSet()
        val wasSaved = projectTitle in savedIds
        if (wasSaved) savedIds.remove(projectTitle) else savedIds.add(projectTitle)
        if (!saveIds(SAVED_PROJECTS_KEY, savedIds)) return
        bookmark.tag = !wasSaved
        bookmark.imageTintList = ContextCompat.getColorStateList(
            this,
            if (wasSaved) R.color.text_sub else R.color.neon_green
        )
        Toast.makeText(
            this,
            if (wasSaved) "Đã bỏ lưu dự án trên thiết bị." else "Đã lưu dự án trên thiết bị.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateProjectBookmarkButton() {
        val projectTitle = findViewById<TextView>(R.id.tvProjectTitle)?.text?.toString() ?: return
        val isSaved = projectTitle in getSavedIds(SAVED_PROJECTS_KEY)
        findViewById<ImageView>(R.id.btnBookmark)?.apply {
            tag = isSaved
            imageTintList = ContextCompat.getColorStateList(
                this@MainActivity,
                if (isSaved) R.color.neon_green else R.color.text_sub
            )
        }
    }

    private fun toggleSavedCreator() {
        val savedIds = getSavedIds(SAVED_CREATORS_KEY).toMutableSet()
        val creatorId = selectedDemoCreator.id
        val wasSaved = creatorId in savedIds
        if (wasSaved) savedIds.remove(creatorId) else savedIds.add(creatorId)
        if (!saveIds(SAVED_CREATORS_KEY, savedIds)) return
        updatePublicCreatorSaveButton()
        Toast.makeText(
            this,
            if (wasSaved) "Đã bỏ lưu hồ sơ ${selectedDemoCreator.name} trên thiết bị."
            else "Đã lưu hồ sơ ${selectedDemoCreator.name} trên thiết bị.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updatePublicCreatorSaveButton() {
        val isSaved = selectedDemoCreator.id in getSavedIds(SAVED_CREATORS_KEY)
        findViewById<Button>(R.id.btnSavePublicCreator)?.apply {
            text = if (isSaved) "✓ ĐÃ LƯU HỒ SƠ CREATOR" else "☆ LƯU HỒ SƠ CREATOR"
            backgroundTintList = ContextCompat.getColorStateList(
                this@MainActivity,
                if (isSaved) R.color.neon_green else R.color.bg_panel
            )
            setTextColor(
                ContextCompat.getColor(this@MainActivity, if (isSaved) R.color.bg_dark else R.color.text_main)
            )
        }
    }

    private fun toggleCreatorConnection() {
        val connectedIds = getSavedIds(SAVED_CONNECTIONS_KEY).toMutableSet()
        val creatorId = selectedDemoCreator.id
        val wasConnected = creatorId in connectedIds
        if (wasConnected) connectedIds.remove(creatorId) else connectedIds.add(creatorId)
        if (!saveIds(SAVED_CONNECTIONS_KEY, connectedIds)) return
        updateCreatorConnectionButton()
        Toast.makeText(
            this,
            if (wasConnected) "Đã hủy kết nối với ${selectedDemoCreator.name}."
            else "Đã gửi lời mời kết bạn tới ${selectedDemoCreator.name}.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateCreatorConnectionButton() {
        val isConnected = selectedDemoCreator.id in getSavedIds(SAVED_CONNECTIONS_KEY)
        findViewById<Button>(R.id.btnFollowPublicCreator)?.apply {
            tag = isConnected
            text = if (isConnected) "✓ ĐÃ KẾT BẠN" else "＋ KẾT BẠN"
        }
    }

    private fun getSavedIds(key: String): Set<String> {
        val email = currentUserEmail ?: return emptySet()
        return getSharedPreferences(SAVED_ITEMS_PREFERENCES, MODE_PRIVATE)
            .getStringSet("$key:$email", emptySet())
            .orEmpty()
            .toSet()
    }

    private fun saveIds(key: String, ids: Set<String>): Boolean {
        val email = currentUserEmail
        if (email == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu mục này.", Toast.LENGTH_SHORT).show()
            return false
        }
        getSharedPreferences(SAVED_ITEMS_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putStringSet("$key:$email", ids.toSet())
            .apply()
        return true
    }

    private fun openHome() {
        navigationHistory.clear()
        showScreen(homeLayout(), addToHistory = false)
    }

    private fun openTab(layout: Int) {
        navigationHistory.clear()
        showScreen(layout, addToHistory = false)
    }

    private fun homeLayout(): Int =
        if (recruiterMode) R.layout.fragment_recruiter_home else R.layout.fragment_creator_home

    private fun updateNavigationSelection(layout: Int) {
        val creatorTab = when (layout) {
            R.layout.fragment_creator_home -> R.id.navCreatorRadar
            R.layout.fragment_cluster_nodes -> R.id.navCreatorExplore
            R.layout.fragment_creator_profile -> R.id.navCreatorProfile
            else -> null
        }
        val recruiterTab = when (layout) {
            R.layout.fragment_recruiter_home -> R.id.navRecruiterRadar
            R.layout.fragment_saved_bookmarks -> R.id.navRecruiterSaved
            R.layout.activity_company_profile -> R.id.navRecruiterCompany
            else -> null
        }
        listOf(
            R.id.navCreatorRadar,
            R.id.navCreatorExplore,
            R.id.navCreatorProfile,
            R.id.navRecruiterRadar,
            R.id.navRecruiterSaved,
            R.id.navRecruiterCompany
        ).forEach { id ->
            setTabSelected(id, id == creatorTab || id == recruiterTab)
        }

        if (layout == R.layout.fragment_saved_bookmarks) {
            findViewById<TextView>(R.id.tvBtnNavRadar)?.text = "Radar"
            findViewById<TextView>(R.id.tvBtnNavSaved)?.text = "Đã lưu"
            findViewById<TextView>(R.id.tvBtnNavCompany)?.text =
                if (recruiterMode) "Công ty" else "Hồ sơ"
            setTabSelected(R.id.btnNavRadar, false)
            setTabSelected(R.id.btnNavSaved, true)
            setTabSelected(R.id.btnNavCompany, false)
            return
        }

        val selected = when (layout) {
            R.layout.fragment_creator_home, R.layout.fragment_recruiter_home -> R.id.navMap
            R.layout.fragment_cluster_nodes -> R.id.navClusters
            R.layout.fragment_signal_stream -> R.id.navSignal
            R.layout.fragment_inbox, R.layout.activity_chat_detail -> R.id.navMessages
            else -> null
        } ?: return

        val tabs = listOf(
            R.id.navMap to R.id.ivNavMap,
            R.id.navClusters to R.id.ivNavClusters,
            R.id.navSignal to R.id.ivNavSignal,
            R.id.navSaved to R.id.ivNavSaved,
            R.id.navMessages to R.id.ivNavMessages
        )
        tabs.forEach { (tabId, iconId) ->
            val active = tabId == selected
            setTabSelected(tabId, active)
            findViewById<ImageView>(iconId)?.setColorFilter(tabColor(active))
        }
    }

    private fun setTabSelected(tabId: Int, selected: Boolean) {
        val color = tabColor(selected)
        val tab = findViewById<ViewGroup>(tabId) ?: return
        tab.animate().alpha(if (selected) 1f else 0.62f).setDuration(200).start()
        for (index in 0 until tab.childCount) {
            when (val child = tab.getChildAt(index)) {
                is ImageView -> animateIconTint(child, tabColor(!selected), color, 220)
                is TextView -> child.setTextColor(color)
            }
        }
    }

    private fun tabColor(selected: Boolean): Int =
        ContextCompat.getColor(this, if (selected) R.color.neon_green else R.color.text_sub)

    private fun goBack() {
        if (navigationHistory.isNotEmpty()) {
            showScreen(navigationHistory.removeLast(), addToHistory = false)
        } else if (isAuthenticated) {
            finish()
        } else {
            showScreen(R.layout.activity_login, addToHistory = false)
        }
    }

    private fun bind(id: Int, action: () -> Unit) {
        findViewById<View>(id)?.let { view ->
            view.setOnClickListener {
                view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
                }.start()
                action()
            }
        }
    }

    private fun animateIconTint(imageView: ImageView, fromColor: Int, toColor: Int, duration: Long = 200) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        animator.duration = duration
        animator.addUpdateListener { anim ->
            imageView.setColorFilter(anim.animatedValue as Int)
        }
        animator.start()
    }

    private fun animateIconPress(view: View) {
        view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
        }.start()
    }

    private data class UiRow(val title: String, val subtitle: String, val key: String? = null)
    private data class DemoCreator(
        val id: String,
        val name: String,
        val headline: String,
        val school: String,
        val bio: String,
        val skills: String
    )
    private data class DemoProject(
        val title: String,
        val creatorId: String,
        val description: String,
        val techStack: String
    )
    private sealed class DemoMapItem {
        abstract val title: String
        abstract val description: String

        data class Project(val project: DemoProject) : DemoMapItem() {
            override val title: String get() = project.title
            override val description: String get() = project.description
        }

        data class Creator(val creator: DemoCreator) : DemoMapItem() {
            override val title: String get() = creator.name
            override val description: String get() = "${creator.headline} • ${creator.skills}"
        }
    }
    private data class MapDemoPin(
        val item: DemoMapItem,
        val latitudeOffset: Double,
        val longitudeOffset: Double
    )

    private class UiRowsAdapter(
        private val context: Context,
        rows: List<UiRow>,
        private val onClick: (UiRow) -> Unit
    ) : RecyclerView.Adapter<UiRowsAdapter.RowHolder>() {
        private val rows = rows.toMutableList()

        class RowHolder(val label: TextView) : RecyclerView.ViewHolder(label)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
            val density = parent.resources.displayMetrics.density
            val label = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, R.color.text_main))
                textSize = 14f
                setPadding((14 * density).toInt(), (14 * density).toInt(), (14 * density).toInt(), (14 * density).toInt())
                setBackgroundColor(ContextCompat.getColor(context, R.color.bg_panel))
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (10 * density).toInt()
                }
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
            }
            return RowHolder(label)
        }

        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            val row = rows[position]
            holder.label.text = "${row.title}\n${row.subtitle}"
            holder.label.setOnClickListener { onClick(row) }
        }

        override fun getItemCount(): Int = rows.size

        fun addRow(row: UiRow) {
            rows.add(row)
            notifyItemInserted(rows.lastIndex)
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1_500L
        const val SAVED_ITEMS_PREFERENCES = "devspot_saved_items"
        const val SAVED_CREATORS_KEY = "saved_creators"
        const val SAVED_PROJECTS_KEY = "saved_projects"
        const val SAVED_CONNECTIONS_KEY = "saved_creator_connections"
    }
}
