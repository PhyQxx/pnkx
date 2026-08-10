const modules = import.meta.glob('/src/assets/icons/svg/*.svg', { eager: true })

const icons = Object.keys(modules).map(path => {
    const match = path.match(/\/([^/]+)\.svg$/)
    return match ? match[1] : path
})

export default icons
