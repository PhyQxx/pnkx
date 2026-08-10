import hasRole from './permission/hasRole'
import hasPermi from './permission/hasPermi'
import dialogDrag from './dialog/drag'
import dialogDragWidth from './dialog/dragWidth'
import dialogDragHeight from './dialog/dragHeight'
import domDrag from './dom/domDrag'
import clickOutSide from './dom/clickOutSide'

const install = function (app) {
    app.directive('hasRole', hasRole)
    app.directive('hasPermi', hasPermi)
    app.directive('domDrag', domDrag)
    app.directive('clickOutSide', clickOutSide)
    app.directive('dialogDrag', dialogDrag)
    app.directive('dialogDragWidth', dialogDragWidth)
    app.directive('dialogDragHeight', dialogDragHeight)
}

export default install
