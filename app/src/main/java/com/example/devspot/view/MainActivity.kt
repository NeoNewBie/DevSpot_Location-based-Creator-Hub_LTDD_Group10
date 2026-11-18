package com.example.devspot.view

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.recyclerview.widget.RecyclerView
import com.example.devspot.BuildConfig
import com.example.devspot.R
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
                showScreen(R.layout.activity_role_selection, addToHistory = false)
            }
        }, SPLASH_DURATION_MS)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    navigationHistory.isNotEmpty() ->
                        showScreen(navigationHistory.removeLast(), addToHistory = false)
                    currentLayout != R.layout.activity_role_selection ->
                        showScreen(R.layout.activity_role_selection, addToHistory = false)
                    else -> finish()
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
                    showScreen(R.layout.activity_role_selection, addToHistory = false)
                }
                bind(R.id.main) {
                    showScreen(R.layout.activity_role_selection, addToHistory = false)
                }
            }

            R.layout.activity_role_selection -> {
                bind(R.id.btnOpenLogin) { showScreen(R.layout.activity_login) }
                updateRoleSelection()
                bind(R.id.cardCreator) {
                    recruiterMode = false
                    updateRoleSelection()
                    Toast.makeText(this, "Đã chọn vai trò Creator", Toast.LENGTH_SHORT).show()
                }
                bind(R.id.cardRecruiter) {
                    recruiterMode = true
                    updateRoleSelection()
                    Toast.makeText(this, "Đã chọn vai trò Recruiter", Toast.LENGTH_SHORT).show()
                }
                bind(R.id.btnContinue) {
                    if (recruiterMode) {
                        showScreen(R.layout.activity_register_recruiter)
                    } else {
                        showScreen(R.layout.activity_register_creator)
                    }
                }
            }

            R.layout.activity_login -> {
                bind(R.id.btnForgotPassword) { showScreen(R.layout.activity_password_reset) }
                bind(R.id.btnOpenRegistration) { showScreen(R.layout.activity_role_selection) }
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
                    Toast.makeText(this, "Đăng nhập đang ở chế độ xem trước; chưa kết nối máy chủ.", Toast.LENGTH_LONG).show()
                    openHome()
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
                bind(R.id.btnChangeRole) { showScreen(R.layout.activity_role_selection) }
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
                            Toast.makeText(this, "Đăng ký đang ở chế độ xem trước; chưa lưu tài khoản.", Toast.LENGTH_LONG).show()
                            recruiterMode = false
                            openHome()
                        }
                    }
                }
            }

            R.layout.activity_register_recruiter -> {
                bind(R.id.btnChangeRole) { showScreen(R.layout.activity_role_selection) }
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
                            Toast.makeText(this, "Đăng ký đang ở chế độ xem trước; chưa lưu tài khoản.", Toast.LENGTH_LONG).show()
                            recruiterMode = true
                            openHome()
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
                bind(R.id.tvAuthorName) { showScreen(R.layout.activity_creator_public_profile) }
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
                    if (recruiterMode) showScreen(R.layout.activity_interview_dispatch)
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

            R.layout.activity_interview_dispatch -> bind(R.id.btnSendInterview) {
                Toast.makeText(this, "Lời mời chỉ được xem trước; chưa gửi tới ứng viên.", Toast.LENGTH_LONG).show()
                showScreen(R.layout.activity_interview_success)
            }

            R.layout.activity_interview_success -> bind(R.id.btnGoToChat) {
                showScreen(R.layout.activity_chat_detail)
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
                bind(R.id.btnFollowPublicCreator) { toggleButton(R.id.btnFollowPublicCreator, "＋ Theo dõi", "✓ Đang theo dõi") }
                bind(R.id.btnMessagePublicCreator) { showScreen(R.layout.activity_chat_detail) }
                bind(R.id.btnInvitePublicCreator) { showScreen(R.layout.activity_interview_dispatch) }
            }

            R.layout.activity_ui_state -> {
                bind(R.id.btnStateBack) { goBack() }
                bind(R.id.btnStateAction) { goBack() }
            }
        }
    }

    private fun populateLists(layout: Int) {
        when (layout) {
            R.layout.fragment_inbox -> setRows(
                R.id.rvInboxList,
                listOf(
                    UiRow("Nexus AI Labs ✔  •  Đang phỏng vấn", "Bạn có thể tham gia phỏng vấn vào thứ 5 không?"),
                    UiRow("Nguyễn Tuấn Kiệt  •  Online", "Mình vừa cập nhật portfolio trên Radar."),
                    UiRow("DevSpot Community", "Chào mừng bạn đến với DevSpot!")
                )
            ) { showScreen(R.layout.activity_chat_detail) }

            R.layout.fragment_signal_stream -> setRows(
                R.id.rvSignalStream,
                listOf(
                    UiRow("LỜI MỜI PHỎNG VẤN • 03h trước", "Techcombank Digital Hub mời bạn trao đổi về vị trí Backend Golang Fresher."),
                    UiRow("DỰ ÁN ĐƯỢC QUAN TÂM • Hôm nay", "Nexus AI Labs đã xem DevSpot Location-Based Hub."),
                    UiRow("KẾT NỐI MỚI • Hôm qua", "Một Recruiter mới đang theo dõi hồ sơ của bạn.")
                )
            ) { showScreen(R.layout.activity_interview_response) }

            R.layout.fragment_saved_bookmarks -> {
                findViewById<View>(R.id.layoutEmpty)?.visibility = View.GONE
                setRows(
                    R.id.rvSavedProjects,
                    listOf(
                        UiRow("Cybersafe IoT / Edge AI", "Nguyễn Tuấn Kiệt • Kotlin • AI/ML"),
                        UiRow("DevSpot Geo Radar", "Lê Anh Quốc • Android • ASP.NET Core")
                    )
                ) { showScreen(R.layout.activity_project_detail) }
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

            R.layout.activity_creator_public_profile -> setRows(
                R.id.rvPublicCreatorProjects,
                listOf(
                    UiRow("Cybersafe IoT / Edge AI", "Kotlin • AI/ML • IoT"),
                    UiRow("Smart Campus Monitor", "Python • MQTT • TensorFlow Lite")
                )
            ) { showScreen(R.layout.activity_project_detail) }

            R.layout.activity_chat_detail -> {
                setRows(
                    R.id.rvChatMessages,
                    listOf(
                        UiRow("Nexus AI Labs", "Chào bạn! Mình muốn trao đổi thêm về dự án của bạn."),
                        UiRow("Bạn", "Cảm ơn bạn, mình sẵn sàng trao đổi.")
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
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
        view.invalidate()
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
        findViewById<TextView>(R.id.btnContinue).text =
            if (recruiterMode) "Tiếp tục với Recruiter →" else "Tiếp tục với Creator →"
        val creatorCard = findViewById<View>(R.id.cardCreator)
        val recruiterCard = findViewById<View>(R.id.cardRecruiter)
        if (recruiterMode) {
            creatorCard?.animate()?.scaleX(0.98f)?.scaleY(0.98f)?.alpha(0.6f)?.setDuration(250)?.start()
            recruiterCard?.animate()?.scaleX(1.02f)?.scaleY(1.02f)?.alpha(1.0f)?.setDuration(250)?.start()
        } else {
            creatorCard?.animate()?.scaleX(1.02f)?.scaleY(1.02f)?.alpha(1.0f)?.setDuration(250)?.start()
            recruiterCard?.animate()?.scaleX(0.98f)?.scaleY(0.98f)?.alpha(0.6f)?.setDuration(250)?.start()
        }
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
        val selected = bookmark.tag == true
        bookmark.tag = !selected
        bookmark.imageTintList = ContextCompat.getColorStateList(
            this,
            if (selected) R.color.neon_blue else R.color.neon_green
        )
        Toast.makeText(
            this,
            if (selected) "Đã bỏ lưu trong phiên xem trước." else "Đã lưu trong phiên xem trước.",
            Toast.LENGTH_SHORT
        ).show()
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

        val selected = when (layout) {
            R.layout.fragment_creator_home, R.layout.fragment_recruiter_home -> R.id.navMap
            R.layout.fragment_cluster_nodes -> R.id.navClusters
            R.layout.fragment_signal_stream -> R.id.navSignal
            R.layout.fragment_saved_bookmarks -> R.id.navSaved
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
        } else {
            showScreen(R.layout.activity_role_selection, addToHistory = false)
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

    private data class UiRow(val title: String, val subtitle: String)

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
    }
}
