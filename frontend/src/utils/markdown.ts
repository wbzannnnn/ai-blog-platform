import { marked } from 'marked'

export interface TocEntry {
  id: string
  text: string
  level: 2 | 3
}

export interface RenderedMarkdown {
  html: string
  toc: TocEntry[]
}

marked.setOptions({ breaks: true, gfm: true })

const blockedElements = 'script, style, iframe, object, embed, form, input, button, textarea, select'

const safeUrl = (value: string): boolean => {
  const normalized = value.trim().toLowerCase()
  return !normalized.startsWith('javascript:') && !normalized.startsWith('data:text/html')
}

export const renderMarkdown = (source: string): RenderedMarkdown => {
  const rawHtml = marked(source) as string
  const documentNode = new DOMParser().parseFromString(rawHtml, 'text/html')
  documentNode.body.querySelectorAll(blockedElements).forEach((element) => element.remove())

  documentNode.body.querySelectorAll<HTMLElement>('*').forEach((element) => {
    for (const attribute of [...element.attributes]) {
      const name = attribute.name.toLowerCase()
      if (name.startsWith('on') || name === 'style') element.removeAttribute(attribute.name)
      if ((name === 'href' || name === 'src') && !safeUrl(attribute.value)) element.removeAttribute(attribute.name)
    }
    if (element.tagName === 'A') {
      element.setAttribute('rel', 'noopener noreferrer')
    }
  })

  const toc: TocEntry[] = []
  const usedIds = new Set<string>()
  documentNode.body.querySelectorAll<HTMLHeadingElement>('h2, h3').forEach((heading, index) => {
    const level = Number(heading.tagName.slice(1)) as 2 | 3
    const text = heading.textContent?.trim() || `章节 ${index + 1}`
    const base = text.toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '') || `section-${index + 1}`
    let id = base
    let suffix = 2
    while (usedIds.has(id)) {
      id = `${base}-${suffix}`
      suffix += 1
    }
    usedIds.add(id)
    heading.id = id
    toc.push({ id, text, level })
  })

  return { html: documentNode.body.innerHTML, toc }
}
