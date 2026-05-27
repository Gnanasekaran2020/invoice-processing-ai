import {
  NzIconDirective,
  NzIconModule
} from "./chunk-F65ULK5U.js";
import "./chunk-BQ76GOFF.js";
import {
  NzConfigService,
  WithConfig
} from "./chunk-MYAO4GUU.js";
import "./chunk-DMHQP4GZ.js";
import {
  PlatformModule
} from "./chunk-PGWO72W2.js";
import "./chunk-T5QFSZEW.js";
import "./chunk-D2K23Q3J.js";
import "./chunk-MKUB7OB3.js";
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgModule,
  Output,
  ViewChild,
  ViewEncapsulation$1,
  numberAttribute,
  setClassMetadata,
  ɵɵInputTransformsFeature,
  ɵɵNgOnChangesFeature,
  ɵɵStandaloneFeature,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵdefineComponent,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵnextContext,
  ɵɵprojection,
  ɵɵprojectionDef,
  ɵɵproperty,
  ɵɵqueryRefresh,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵsanitizeUrl,
  ɵɵstyleProp,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵviewQuery
} from "./chunk-L7PJK5KF.js";
import "./chunk-ISM5WLAM.js";
import "./chunk-IC62NIWK.js";
import {
  __decorate
} from "./chunk-ZZ67MR3E.js";
import "./chunk-WDMUDEB6.js";

// node_modules/ng-zorro-antd/fesm2022/ng-zorro-antd-avatar.mjs
var _c0 = ["textEl"];
function NzAvatarComponent_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 1);
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext();
    ɵɵproperty("nzType", ctx_r0.nzIcon);
  }
}
function NzAvatarComponent_Conditional_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r2 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "img", 4);
    ɵɵlistener("error", function NzAvatarComponent_Conditional_1_Template_img_error_0_listener($event) {
      ɵɵrestoreView(_r2);
      const ctx_r0 = ɵɵnextContext();
      return ɵɵresetView(ctx_r0.imgError($event));
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext();
    ɵɵproperty("src", ctx_r0.nzSrc, ɵɵsanitizeUrl);
    ɵɵattribute("srcset", ctx_r0.nzSrcSet)("alt", ctx_r0.nzAlt);
  }
}
function NzAvatarComponent_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 3, 0);
    ɵɵtext(2);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext();
    ɵɵadvance(2);
    ɵɵtextInterpolate(ctx_r0.nzText);
  }
}
var _c1 = ["*"];
var NZ_CONFIG_MODULE_NAME = "avatar";
var NzAvatarComponent = class _NzAvatarComponent {
  constructor(nzConfigService, elementRef, cdr) {
    this.nzConfigService = nzConfigService;
    this.elementRef = elementRef;
    this.cdr = cdr;
    this._nzModuleName = NZ_CONFIG_MODULE_NAME;
    this.nzShape = "circle";
    this.nzSize = "default";
    this.nzGap = 4;
    this.nzError = new EventEmitter();
    this.hasText = false;
    this.hasSrc = true;
    this.hasIcon = false;
    this.classMap = {};
    this.customSize = null;
    this.el = this.elementRef.nativeElement;
  }
  imgError($event) {
    this.nzError.emit($event);
    if (!$event.defaultPrevented) {
      this.hasSrc = false;
      this.hasIcon = false;
      this.hasText = false;
      if (this.nzIcon) {
        this.hasIcon = true;
      } else if (this.nzText) {
        this.hasText = true;
      }
      this.cdr.detectChanges();
      this.setSizeStyle();
      this.calcStringSize();
    }
  }
  ngOnChanges() {
    this.hasText = !this.nzSrc && !!this.nzText;
    this.hasIcon = !this.nzSrc && !!this.nzIcon;
    this.hasSrc = !!this.nzSrc;
    this.setSizeStyle();
    this.calcStringSize();
  }
  ngAfterViewInit() {
    this.calcStringSize();
  }
  calcStringSize() {
    if (!this.hasText || !this.textEl) {
      return;
    }
    const textEl = this.textEl.nativeElement;
    const childrenWidth = textEl.offsetWidth;
    const avatarWidth = this.el.getBoundingClientRect?.().width ?? 0;
    const offset = this.nzGap * 2 < avatarWidth ? this.nzGap * 2 : 8;
    const scale = avatarWidth - offset < childrenWidth ? (avatarWidth - offset) / childrenWidth : 1;
    textEl.style.transform = `scale(${scale}) translateX(-50%)`;
    textEl.style.lineHeight = this.customSize || "";
  }
  setSizeStyle() {
    if (typeof this.nzSize === "number") {
      this.customSize = `${this.nzSize}px`;
    } else {
      this.customSize = null;
    }
    this.cdr.markForCheck();
  }
  static {
    this.ɵfac = function NzAvatarComponent_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NzAvatarComponent)(ɵɵdirectiveInject(NzConfigService), ɵɵdirectiveInject(ElementRef), ɵɵdirectiveInject(ChangeDetectorRef));
    };
  }
  static {
    this.ɵcmp = ɵɵdefineComponent({
      type: _NzAvatarComponent,
      selectors: [["nz-avatar"]],
      viewQuery: function NzAvatarComponent_Query(rf, ctx) {
        if (rf & 1) {
          ɵɵviewQuery(_c0, 5);
        }
        if (rf & 2) {
          let _t;
          ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.textEl = _t.first);
        }
      },
      hostAttrs: [1, "ant-avatar"],
      hostVars: 20,
      hostBindings: function NzAvatarComponent_HostBindings(rf, ctx) {
        if (rf & 2) {
          ɵɵstyleProp("width", ctx.customSize)("height", ctx.customSize)("line-height", ctx.customSize)("font-size", ctx.hasIcon && ctx.customSize ? ctx.nzSize / 2 : null, "px");
          ɵɵclassProp("ant-avatar-lg", ctx.nzSize === "large")("ant-avatar-sm", ctx.nzSize === "small")("ant-avatar-square", ctx.nzShape === "square")("ant-avatar-circle", ctx.nzShape === "circle")("ant-avatar-icon", ctx.nzIcon)("ant-avatar-image", ctx.hasSrc);
        }
      },
      inputs: {
        nzShape: "nzShape",
        nzSize: "nzSize",
        nzGap: [2, "nzGap", "nzGap", numberAttribute],
        nzText: "nzText",
        nzSrc: "nzSrc",
        nzSrcSet: "nzSrcSet",
        nzAlt: "nzAlt",
        nzIcon: "nzIcon"
      },
      outputs: {
        nzError: "nzError"
      },
      exportAs: ["nzAvatar"],
      standalone: true,
      features: [ɵɵInputTransformsFeature, ɵɵNgOnChangesFeature, ɵɵStandaloneFeature],
      decls: 3,
      vars: 3,
      consts: [["textEl", ""], ["nz-icon", "", 3, "nzType"], [3, "src"], [1, "ant-avatar-string"], [3, "error", "src"]],
      template: function NzAvatarComponent_Template(rf, ctx) {
        if (rf & 1) {
          ɵɵtemplate(0, NzAvatarComponent_Conditional_0_Template, 1, 1, "span", 1)(1, NzAvatarComponent_Conditional_1_Template, 1, 3, "img", 2)(2, NzAvatarComponent_Conditional_2_Template, 3, 1, "span", 3);
        }
        if (rf & 2) {
          ɵɵconditional(ctx.nzIcon && ctx.hasIcon ? 0 : -1);
          ɵɵadvance();
          ɵɵconditional(ctx.nzSrc && ctx.hasSrc ? 1 : -1);
          ɵɵadvance();
          ɵɵconditional(ctx.nzText && ctx.hasText ? 2 : -1);
        }
      },
      dependencies: [NzIconModule, NzIconDirective, PlatformModule],
      encapsulation: 2,
      changeDetection: 0
    });
  }
};
__decorate([WithConfig()], NzAvatarComponent.prototype, "nzShape", void 0);
__decorate([WithConfig()], NzAvatarComponent.prototype, "nzSize", void 0);
__decorate([WithConfig()], NzAvatarComponent.prototype, "nzGap", void 0);
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NzAvatarComponent, [{
    type: Component,
    args: [{
      selector: "nz-avatar",
      exportAs: "nzAvatar",
      standalone: true,
      imports: [NzIconModule, PlatformModule],
      template: `
    @if (nzIcon && hasIcon) {
      <span nz-icon [nzType]="nzIcon"></span>
    }
    @if (nzSrc && hasSrc) {
      <img [src]="nzSrc" [attr.srcset]="nzSrcSet" [attr.alt]="nzAlt" (error)="imgError($event)" />
    }
    @if (nzText && hasText) {
      <span class="ant-avatar-string" #textEl>{{ nzText }}</span>
    }
  `,
      host: {
        class: "ant-avatar",
        "[class.ant-avatar-lg]": `nzSize === 'large'`,
        "[class.ant-avatar-sm]": `nzSize === 'small'`,
        "[class.ant-avatar-square]": `nzShape === 'square'`,
        "[class.ant-avatar-circle]": `nzShape === 'circle'`,
        "[class.ant-avatar-icon]": `nzIcon`,
        "[class.ant-avatar-image]": `hasSrc `,
        "[style.width]": "customSize",
        "[style.height]": "customSize",
        "[style.line-height]": "customSize",
        // nzSize type is number when customSize is true
        "[style.font-size.px]": "(hasIcon && customSize) ? $any(nzSize) / 2 : null"
      },
      preserveWhitespaces: false,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation$1.None
    }]
  }], () => [{
    type: NzConfigService
  }, {
    type: ElementRef
  }, {
    type: ChangeDetectorRef
  }], {
    nzShape: [{
      type: Input
    }],
    nzSize: [{
      type: Input
    }],
    nzGap: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    nzText: [{
      type: Input
    }],
    nzSrc: [{
      type: Input
    }],
    nzSrcSet: [{
      type: Input
    }],
    nzAlt: [{
      type: Input
    }],
    nzIcon: [{
      type: Input
    }],
    nzError: [{
      type: Output
    }],
    textEl: [{
      type: ViewChild,
      args: ["textEl", {
        static: false
      }]
    }]
  });
})();
var NzAvatarGroupComponent = class _NzAvatarGroupComponent {
  static {
    this.ɵfac = function NzAvatarGroupComponent_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NzAvatarGroupComponent)();
    };
  }
  static {
    this.ɵcmp = ɵɵdefineComponent({
      type: _NzAvatarGroupComponent,
      selectors: [["nz-avatar-group"]],
      hostAttrs: [1, "ant-avatar-group"],
      exportAs: ["nzAvatarGroup"],
      standalone: true,
      features: [ɵɵStandaloneFeature],
      ngContentSelectors: _c1,
      decls: 1,
      vars: 0,
      template: function NzAvatarGroupComponent_Template(rf, ctx) {
        if (rf & 1) {
          ɵɵprojectionDef();
          ɵɵprojection(0);
        }
      },
      encapsulation: 2,
      changeDetection: 0
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NzAvatarGroupComponent, [{
    type: Component,
    args: [{
      selector: "nz-avatar-group",
      exportAs: "nzAvatarGroup",
      standalone: true,
      template: ` <ng-content></ng-content> `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      host: {
        class: "ant-avatar-group"
      }
    }]
  }], null, null);
})();
var NzAvatarModule = class _NzAvatarModule {
  static {
    this.ɵfac = function NzAvatarModule_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NzAvatarModule)();
    };
  }
  static {
    this.ɵmod = ɵɵdefineNgModule({
      type: _NzAvatarModule,
      imports: [NzAvatarComponent, NzAvatarGroupComponent],
      exports: [NzAvatarComponent, NzAvatarGroupComponent]
    });
  }
  static {
    this.ɵinj = ɵɵdefineInjector({
      imports: [NzAvatarComponent]
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NzAvatarModule, [{
    type: NgModule,
    args: [{
      exports: [NzAvatarComponent, NzAvatarGroupComponent],
      imports: [NzAvatarComponent, NzAvatarGroupComponent]
    }]
  }], null, null);
})();
export {
  NzAvatarComponent,
  NzAvatarGroupComponent,
  NzAvatarModule
};
//# sourceMappingURL=ng-zorro-antd_avatar.js.map
