<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <span>AI博客系统</span>
      <span>© {{ currentYear }} 独立技术写作与实践记录</span>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  width: min(calc(100% - var(--space-8)), var(--content-width));
  margin: 0 auto;
  padding: var(--space-8) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

@media (max-width: 640px) {
  .footer-inner {
    flex-direction: column;
    gap: var(--space-2);
    width: min(calc(100% - var(--space-6)), var(--content-width));
  }
}
</style>
