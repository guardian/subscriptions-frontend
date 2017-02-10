const SHOW_CLASS = 'js-show';


function animationFinishHandler(element) {
    element.addEventListener('animationend', (event) => {
        event.preventDefault();
        element.classList.remove('animate-hide');
        element.classList.remove('animate-show');
        if (event.animationName === 'hide') {
            element.classList.add('animate-hide-hidden')
        } else if (event.animationName === 'show') {
            element.classList.remove('animate-hide-hidden')
        }
    })
}

function childAnimationFinishHandler(element) {
    element.addEventListener('animationend', (event) => {
            console.log(event, element)
        element.classList.remove('animate-hide');
        element.classList.remove('animate-show');
            if (event.animationName === 'hideChild') {
                element.classList.add('animate-hide-hidden')
            }
        }
    )
}

function show(element) {
    element.classList.remove('animate-hide');
    element.classList.add('animate-show');
    element.classList.remove('animate-hide-hidden')
}

export function init() {
    console.log('hello');
    debugger;
    let elements = [...document.querySelectorAll('.js-hide')];
console.log(elements);
    elements.map((element) => {
        let otherElements = elements.filter((e) => {
            return e !== element
        });
        let children = [...document.querySelectorAll('.js-dropdown-' + element.dataset.dropdownMenu)];
        console.warn(children);
        element.addEventListener('click', (event) => {
            console.log(event,element);
            if (element.classList.contains(SHOW_CLASS)) {
                console.log('trying to hide',children);
                element.classList.remove(SHOW_CLASS);
                otherElements.map(show);
                console.log(children);
                children.map((e) => {
                    e.classList.add('animate-hide');
                });
                console.log(children)
            } else {
                children.map(show);
                element.classList.add(SHOW_CLASS);
                otherElements.map((e) => {
                    e.classList.add('animate-hide');
                })
            }
        });
        animationFinishHandler(element);
        children.map(animationFinishHandler)
    });
}
