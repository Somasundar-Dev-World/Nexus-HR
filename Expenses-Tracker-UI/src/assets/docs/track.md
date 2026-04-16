# Masterclass: Angular 17 Architecture & Standalone Components

This system is built using Angular 17, representing a massive shift in how Angular applications are structured compared to earlier versions (v14 and below).

## 1. Standalone Components Explained
Historically, Angular required \`@NgModule\` to declare components and manage dependencies. Angular 17 introduces **Standalone Components** as the default.

\`\`\`typescript
@Component({
  selector: 'app-omni-dashboard',
  standalone: true, // No NgModule Required!
  imports: [CommonModule, FormsModule, RouterLink], // Import specific dependencies directly
  templateUrl: './omni-dashboard.component.html'
})
export class OmniDashboardComponent { ... }
\`\`\`
By setting \`standalone: true\`, the component imports exactly what it needs (like \`CommonModule\` for \`*ngIf\` or \`FormsModule\` for \`[(ngModel)]\`). This enables aggressive tree-shaking by Webpack, severely reducing the initial JavaScript bundle size.

## 2. Advanced State Management (Local)

The \`OmniDashboardComponent\` is highly dynamic. Instead of using complex deep-routing (which forces total page re-renders), it acts as a **Local State Machine**.

\`\`\`typescript
// The State Machine
type ViewMode = 'APP_GRID' | 'TRACKER_GRID' | 'TRACKER_DETAIL' | 'DEEP_RESEARCH';
viewMode: ViewMode = 'APP_GRID';

// Navigation functions don't hit the Router, they just flip the state
openDeepResearch() {
  this.viewMode = 'DEEP_RESEARCH';
}
\`\`\`

In the HTML template, the physical DOM is rapidly swapped using Structural Directives:
\`\`\`html
<!-- Structural Directives control DOM instantiation -->
<div *ngIf="viewMode === 'APP_GRID'">
  <app-grid-view></app-grid-view>
</div>

<div *ngIf="viewMode === 'DEEP_RESEARCH'">
  <deep-research-view></deep-research-view>
</div>
\`\`\`

## 3. Dealing with DOM Race Conditions

A common danger in Angular is the **DOM Race Condition**. Because \`*ngIf\` physically adds/removes elements from the DOM, querying those elements immediately after changing a state variable will fail.

\`\`\`typescript
// THE DANGER:
this.viewMode = 'REPORT_VIEW';
document.querySelector('#my-chart'); // DANGER! Returns null, Angular hasn't updated the DOM yet!

// THE SOLUTION: (Micro-task queues)
this.viewMode = 'REPORT_VIEW';
setTimeout(() => {
  // Executes in the next tick of the Event Loop, AFTER Angular's Change Detection
  document.querySelector('#my-chart'); // Success!
}, 50);
\`\`\`
We use this \`setTimeout\` trick whenever initializing external libraries (like ApexCharts) that demand a physical HTML element to attach to.
