// ═══ PROJECT FILTERING ═══
document.addEventListener('DOMContentLoaded', () => {
    const filterButtons = document.querySelectorAll('.filter-btn');
    const projectCards = document.querySelectorAll('.project-card');

    filterButtons.forEach(button => {
        button.addEventListener('click', () => {
            // Remove active class from all buttons
            filterButtons.forEach(btn => btn.classList.remove('active'));
            // Add active class to clicked button
            button.classList.add('active');

            const filterValue = button.getAttribute('data-filter');

            projectCards.forEach(card => {
                const categories = card.getAttribute('data-category').split(' ');
                
                if (filterValue === 'all' || categories.includes(filterValue)) {
                    card.style.display = 'block';
                    setTimeout(() => {
                        card.style.opacity = '1';
                        card.style.transform = 'translateY(0)';
                    }, 50);
                } else {
                    card.style.opacity = '0';
                    card.style.transform = 'translateY(20px)';
                    setTimeout(() => {
                        card.style.display = 'none';
                    }, 300);
                }
            });
        });
    });

    // ═══ FORM SUBMISSION ACTIVATION (Formspree) ═══
    const contactForm = document.getElementById('contact-form');
    const formStatus = document.getElementById('form-status');
    const closeStatusBtn = document.getElementById('close-status');

    if (contactForm) {
        contactForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitBtn = document.getElementById('submit-btn');
            const originalText = submitBtn.innerText;
            const formData = new FormData(contactForm);

            // UI: Sending State
            submitBtn.innerText = 'Sending Portfolio Inquiry...';
            submitBtn.disabled = true;

            try {
                const response = await fetch(contactForm.action, {
                    method: contactForm.method,
                    body: formData,
                    headers: { 'Accept': 'application/json' }
                });

                if (response.ok) {
                    // Success: Show Premium Modal
                    formStatus.classList.add('active');
                    contactForm.reset();

                    // GA4: Track Lead Conversion
                    if (typeof gtag === 'function') {
                        gtag('event', 'generate_lead', {
                            'event_category': 'Engagement',
                            'event_label': 'Contact Form Success'
                        });
                    }
                } else {
                    const data = await response.json();
                    alert(data.errors ? data.errors.map(error => error.message).join(", ") : "Oops! There was a problem submitting your form.");
                }
            } catch (error) {
                alert("Network error. Please try again or email me directly at mr.somasundar77@gmail.com");
            } finally {
                submitBtn.innerText = originalText;
                submitBtn.disabled = false;
            }
        });
    }

    if (closeStatusBtn) {
        closeStatusBtn.addEventListener('click', () => {
            formStatus.classList.remove('active');
        });
    }

    // ═══ GA4: TRACK RESUME CLICKS ═══
    const resumeBtn = document.querySelector('.btn-nav');
    if (resumeBtn) {
        resumeBtn.addEventListener('click', () => {
            if (typeof gtag === 'function') {
                gtag('event', 'view_resume', {
                    'event_category': 'Engagement',
                    'event_label': 'LinkedIn Resume Click'
                });
            }
        });
    }

    // ═══ SMOOTH SCROLL ENHANCEMENT ═══
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                const offset = 100; // Account for glass nav
                const bodyRect = document.body.getBoundingClientRect().top;
                const elementRect = target.getBoundingClientRect().top;
                const elementPosition = elementRect - bodyRect;
                const offsetPosition = elementPosition - offset;

                window.scrollTo({
                    top: offsetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });

    // ═══ REVEAL ANIMATIONS ON SCROLL ═══
    const revealElements = document.querySelectorAll('.bento-item, .project-card, .blog-card');
    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                revealObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    revealElements.forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'all 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
        revealObserver.observe(el);
    });
});
