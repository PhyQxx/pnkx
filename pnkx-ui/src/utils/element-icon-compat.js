import { h, render } from 'vue'
import iconList from '@/utils/generator/icon.json'

const directIconAliases = {
    'platform-eleme': 'ElemeFilled',
    'delete-solid': 'DeleteFilled',
    'user-solid': 'UserFilled',
    'phone-outline': 'Phone',
    'more-outline': 'More',
    'star-on': 'StarFilled',
    'star-off': 'Star',
    's-tools': 'Tools',
    's-goods': 'GoodsFilled',
    'warning-outline': 'Warning',
    'question': 'QuestionFilled',
    'info': 'InfoFilled',
    'success': 'SuccessFilled',
    'error': 'CircleCloseFilled',
    'remove-outline': 'Remove',
    'circle-plus-outline': 'CirclePlus',
    's-help': 'HelpFilled',
    'picture-outline': 'Picture',
    'picture-outline-round': 'PictureRounded',
    'upload2': 'UploadFilled',
    'camera-solid': 'CameraFilled',
    'video-camera-solid': 'VideoCameraFilled',
    'message-solid': 'MessageBox',
    's-cooperation': 'Connection',
    's-order': 'Tickets',
    's-platform': 'Platform',
    's-fold': 'Fold',
    's-unfold': 'Expand',
    's-operation': 'Operation',
    's-promotion': 'Promotion',
    's-home': 'HomeFilled',
    's-release': 'Promotion',
    's-ticket': 'Ticket',
    's-management': 'Management',
    's-open': 'Open',
    's-shop': 'Shop',
    's-marketing': 'TrendCharts',
    's-flag': 'Flag',
    's-comment': 'Comment',
    's-finance': 'Money',
    's-claim': 'Finished',
    's-custom': 'User',
    's-opportunity': 'Opportunity',
    's-data': 'DataAnalysis',
    's-check': 'Checked',
    's-grid': 'Grid',
    'date': 'Calendar',
    'edit-outline': 'EditPen',
    'attract': 'Aim',
    'mobile': 'Cellphone',
    'scissors': 'Scissor',
    'suitcase-1': 'SuitcaseLine',
    'notebook-1': 'Notebook',
    'notebook-2': 'Memo',
    'table-lamp': 'ReadingLamp',
    'shopping-cart-1': 'ShoppingCart',
    'shopping-cart-2': 'ShoppingTrolley',
    'shopping-bag-1': 'ShoppingBag',
    'shopping-bag-2': 'Handbag',
    'bank-card': 'CreditCard',
    'news': 'Memo',
    'turn-off-microphone': 'Mute',
    'close-notification': 'MuteNotification',
    'bangzhu': 'Help',
    'time': 'Clock',
    'medal-1': 'GoldMedal',
    'trophy-1': 'TrophyBase',
    'location-outline': 'Location',
    'mobile-phone': 'Iphone',
    'heavy-rain': 'Pouring',
    'cloudy-and-sunny': 'MostlyCloudy',
    'dish-1': 'DishDot',
    'water-cup': 'Mug',
    'potato-strips': 'Fries',
    'c-scale-to-original': 'ScaleToOriginal'
}

const additionalLegacyIconNames = [
    'arrow-right',
    'caret-bottom',
    'close',
    'hide',
    'loading',
    'lock',
    'menu',
    'plus',
    'delete',
    'document',
    'edit',
    'search',
    'user',
    'view',
    'wallet'
]

const legacyIconNamePattern = /^el-icon-(?!-)(.+)$/

function toPascalCase(name) {
    return name
        .split('-')
        .filter(Boolean)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join('')
}

function resolveLegacyIconComponent(iconComponents, name) {
    const candidates = [
        directIconAliases[name],
        toPascalCase(name),
        name.endsWith('-outline') ? toPascalCase(name.replace(/-outline$/, '')) : '',
        name.endsWith('-solid') ? `${toPascalCase(name.replace(/-solid$/, ''))}Filled` : '',
        name.startsWith('s-') ? `${toPascalCase(name.slice(2))}Filled` : '',
        name.startsWith('s-') ? toPascalCase(name.slice(2)) : '',
        /\-\d+$/.test(name) ? toPascalCase(name.replace(/\-\d+$/, '')) : ''
    ].filter(Boolean)

    return candidates.map(candidate => iconComponents[candidate]).find(Boolean)
}

function getLegacyIconNames() {
    return Array.from(new Set([...iconList, ...additionalLegacyIconNames]))
}

function getLegacyIconNameFromElement(el, iconComponents) {
    return Array.from(el.classList || [])
        .map(className => className.match(legacyIconNamePattern)?.[1])
        .filter(Boolean)
        .find(name => resolveLegacyIconComponent(iconComponents, name))
}

function renderLegacyIconElement(el, iconComponents) {
    const iconName = getLegacyIconNameFromElement(el, iconComponents)

    if (!iconName) {
        if (el.__legacyElementIconName) {
            render(null, el)
            delete el.__legacyElementIconName
        }
        el.classList.remove('el-icon-compat', 'el-icon-compat--loading')
        return
    }

    if (el.__legacyElementIconName === iconName) {
        return
    }

    const component = resolveLegacyIconComponent(iconComponents, iconName)
    render(h(component), el)
    el.__legacyElementIconName = iconName
    el.classList.add('el-icon-compat')
    el.classList.toggle('el-icon-compat--loading', iconName === 'loading')
}

function renderLegacyIcons(root, iconComponents) {
    if (!root || root.nodeType !== Node.ELEMENT_NODE) {
        return
    }

    if (root.matches?.('[class*="el-icon-"]')) {
        renderLegacyIconElement(root, iconComponents)
    }

    root.querySelectorAll?.('[class*="el-icon-"]').forEach(el => {
        renderLegacyIconElement(el, iconComponents)
    })
}

export function registerLegacyElementIconAliases(app, iconComponents) {
    getLegacyIconNames().forEach(name => {
        const component = resolveLegacyIconComponent(iconComponents, name)

        if (component) {
            app.component(`el-icon-${name}`, component)
        }
    })
}

export function startLegacyElementIconObserver(iconComponents, root = document.getElementById('app')) {
    renderLegacyIcons(root, iconComponents)

    const observeOptions = {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['class']
    }

    const observer = new MutationObserver(mutations => {
        observer.disconnect()

        mutations.forEach(mutation => {
            if (mutation.type === 'attributes') {
                renderLegacyIconElement(mutation.target, iconComponents)
                return
            }

            mutation.addedNodes.forEach(node => {
                renderLegacyIcons(node, iconComponents)
            })
        })

        observer.observe(root || document.body, observeOptions)
    })

    observer.observe(root || document.body, observeOptions)

    return observer
}

